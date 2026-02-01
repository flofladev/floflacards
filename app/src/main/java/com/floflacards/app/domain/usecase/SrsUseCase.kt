/*
 * Copyright (C) 2026 FloFla Dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.floflacards.app.domain.usecase

import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.repository.FlashcardRepository
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.domain.model.FlashcardRating
import com.floflacards.app.domain.model.Sm2Config
import com.floflacards.app.domain.model.Sm2Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class SrsUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val settingsManager: SettingsRepository
) {
    private val sm2Config = Sm2Config.getDefault()
    
    /**
     * Calculates the new SM-2 state for a flashcard based on user rating.
     * Implements the SM-2 algorithm adapted to work with user-defined intervals.
     */
    suspend fun calculateSm2Update(
        flashcard: FlashcardEntity,
        rating: FlashcardRating
    ): Result<Sm2Result> = withContext(Dispatchers.Default) {
        try {
            val currentTime = System.currentTimeMillis()
            val intervalMinutes = settingsManager.getIntervalMinutes()
            
            // Map user rating to SM-2 quality (0-5 scale)
            val quality = mapRatingToQuality(rating)
            
            val sm2Result = if (quality < 3) {
                // Failed review - apply progressive penalty based on recent failures
                val failedCardCooldown = calculateFailedCardCooldown(flashcard)
                Sm2Result(
                    cooldownUntilTimestamp = currentTime + calculateIntervalMs(failedCardCooldown, intervalMinutes),
                    newEasinessFactor = flashcard.easinessFactor, // Don't change EF for failed cards
                    newReviewCount = 0, // Reset review count
                    shouldUpdateStats = rating != FlashcardRating.CLOSED
                )
            } else {
                // Successful review - apply SM-2 algorithm
                val newEasinessFactor = calculateNewEasinessFactor(flashcard.easinessFactor, quality)
                val newReviewCount = flashcard.reviewCount + 1
                val intervalMultiplier = calculateIntervalMultiplier(newReviewCount, newEasinessFactor)
                
                Sm2Result(
                    cooldownUntilTimestamp = currentTime + calculateIntervalMs(intervalMultiplier, intervalMinutes),
                    newEasinessFactor = newEasinessFactor,
                    newReviewCount = newReviewCount,
                    shouldUpdateStats = rating != FlashcardRating.CLOSED
                )
            }
            
            Result.success(sm2Result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Maps user rating to SM-2 quality scale (0-5)
     */
    private fun mapRatingToQuality(rating: FlashcardRating): Int {
        return when (rating) {
            FlashcardRating.WRONG -> 0   // Complete failure
            FlashcardRating.HARD -> 3    // Correct with difficulty
            FlashcardRating.GOOD -> 5    // Perfect response
            FlashcardRating.CLOSED -> 2  // Skip - treat as barely correct
        }
    }
    
    /**
     * Calculates new easiness factor based on SM-2 algorithm
     */
    private fun calculateNewEasinessFactor(currentEF: Float, quality: Int): Float {
        val newEF = currentEF + (
            sm2Config.easinessFactorAdjustment - 
            (5 - quality) * (sm2Config.qualityPenalty + (5 - quality) * sm2Config.qualityPenaltySquared)
        )
        
        return max(sm2Config.minEasinessFactor, newEF)
    }
    
    /**
     * Calculates interval multiplier based on review count and easiness factor
     */
    private fun calculateIntervalMultiplier(reviewCount: Int, easinessFactor: Float): Int {
        return when (reviewCount) {
            1 -> sm2Config.firstReviewInterval
            2 -> sm2Config.secondReviewInterval
            else -> {
                // For subsequent reviews: previous_interval * easiness_factor
                val previousInterval = if (reviewCount == 3) {
                    sm2Config.secondReviewInterval
                } else {
                    // Approximate previous interval for higher review counts
                    (sm2Config.secondReviewInterval * Math.pow(easinessFactor.toDouble(), (reviewCount - 2).toDouble())).roundToInt()
                }
                (previousInterval * easinessFactor).roundToInt()
            }
        }
    }
    
    /**
     * Calculates failed card cooldown with progressive penalty.
     * Cards that fail repeatedly get longer cooldowns to prevent infinite loops.
     */
    private fun calculateFailedCardCooldown(flashcard: FlashcardEntity): Int {
        // Calculate failure rate to determine penalty
        val totalAttempts = flashcard.correctCount + flashcard.incorrectCount
        val failureRate = if (totalAttempts > 0) {
            flashcard.incorrectCount.toFloat() / totalAttempts.toFloat()
        } else {
            0.5f // Default for new cards
        }
        
        // Progressive penalty based on failure rate and easiness factor
        val baseCooldown = sm2Config.baseFailedCardInterval
        val penaltyMultiplier = when {
            failureRate >= 0.8f -> 4  // Very hard cards: 8 intervals
            failureRate >= 0.6f -> 3  // Hard cards: 6 intervals  
            failureRate >= 0.4f -> 2  // Moderate cards: 4 intervals
            else -> 1              // Easy cards: 2 intervals
        }
        
        val calculatedCooldown = baseCooldown * penaltyMultiplier
        return min(calculatedCooldown, sm2Config.maxFailedCardInterval)
    }
    
    /**
     * Converts interval multiplier to milliseconds
     */
    private fun calculateIntervalMs(intervalMultiplier: Int, intervalMinutes: Int): Long {
        return intervalMultiplier.toLong() * intervalMinutes * 60 * 1000
    }
    
    /**
     * Updates a flashcard based on user rating and SRS calculation
     */
    suspend fun updateFlashcardRating(
        flashcard: FlashcardEntity,
        rating: FlashcardRating
    ): Result<FlashcardEntity> = withContext(Dispatchers.IO) {
        try {
            val sm2Result = calculateSm2Update(flashcard, rating).getOrThrow()
            val currentTime = System.currentTimeMillis()
            
            val updatedFlashcard = flashcard.copy(
                cooldownUntil = sm2Result.cooldownUntilTimestamp,
                easinessFactor = sm2Result.newEasinessFactor,
                reviewCount = sm2Result.newReviewCount,
                correctCount = if (rating == FlashcardRating.GOOD && sm2Result.shouldUpdateStats) 
                    flashcard.correctCount + 1 else flashcard.correctCount,
                incorrectCount = if (rating == FlashcardRating.WRONG && sm2Result.shouldUpdateStats) 
                    flashcard.incorrectCount + 1 else flashcard.incorrectCount,
                // hardCount should increment every time HARD is pressed, independent of SM-2 stats logic
                hardCount = if (rating == FlashcardRating.HARD) 
                    flashcard.hardCount + 1 else flashcard.hardCount,
                lastReviewedAt = if (sm2Result.shouldUpdateStats) currentTime else flashcard.lastReviewedAt,
                updatedAt = currentTime
            )
            
            repository.updateFlashcard(updatedFlashcard)
            Result.success(updatedFlashcard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
