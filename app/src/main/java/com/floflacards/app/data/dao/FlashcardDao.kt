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

package com.floflacards.app.data.dao

import androidx.room.*
import com.floflacards.app.data.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

/** Projection for [FlashcardDao.getFlashcardCountsPerCategory]: how many flashcards a category holds. */
data class CategoryFlashcardCount(
    val categoryId: Long,
    val count: Int
)

@Dao
interface FlashcardDao {
    
    @Query("SELECT * FROM flashcards WHERE categoryId = :categoryId ORDER BY createdAt ASC")
    fun getFlashcardsByCategory(categoryId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE categoryId = :categoryId ORDER BY createdAt ASC")
    suspend fun getFlashcardsByCategorySync(categoryId: Long): List<FlashcardEntity>
    
    @Query("""
        SELECT f.* FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
        ORDER BY f.createdAt ASC
    """)
    suspend fun getAllFlashcards(): List<FlashcardEntity>
    
    @Query("""
        SELECT f.* FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        ORDER BY f.createdAt ASC
    """)
    suspend fun getAllFlashcardsForStatistics(): List<FlashcardEntity>
    
    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getFlashcardById(id: Long): FlashcardEntity?
    
    @Query("""
        SELECT f.* FROM flashcards f
        INNER JOIN categories c ON f.categoryId = c.id
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
        AND f.cooldownUntil <= :currentTime
        AND f.id != :excludeId
        ORDER BY
            (f.cooldownUntil - f.lastReviewedAt) DESC,
            f.easinessFactor ASC,
            CASE WHEN (f.correctCount + f.incorrectCount) = 0 THEN 0.5
                 ELSE CAST(f.incorrectCount AS REAL) / (f.correctCount + f.incorrectCount)
            END DESC,
            RANDOM()
        LIMIT 1
    """)
    suspend fun getNextFlashcardForReview(
        currentTime: Long = System.currentTimeMillis(),
        excludeId: Long = NO_EXCLUDED_CARD
    ): FlashcardEntity?

    @Query("""
        SELECT f.* FROM flashcards f
        INNER JOIN categories c ON f.categoryId = c.id
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
        AND f.id != :excludeId
        ORDER BY
            f.cooldownUntil ASC,
            f.easinessFactor ASC,
            CASE WHEN (f.correctCount + f.incorrectCount) = 0 THEN 0.5
                 ELSE CAST(f.incorrectCount AS REAL) / (f.correctCount + f.incorrectCount)
            END DESC,
            RANDOM()
        LIMIT 1
    """)
    suspend fun getCardWithShortestCooldown(excludeId: Long = NO_EXCLUDED_CARD): FlashcardEntity?

    /**
     * Gets the next available flashcard, guaranteeing a result if any cards exist.
     *
     * Tries, in order:
     *  1. A card ready for review that is NOT [excludeId] (the card just shown).
     *  2. The card closest to being ready that is NOT [excludeId].
     *  3. As a last resort, [excludeId] itself — reached only when it is the sole
     *     enabled card (e.g. a single-card deck), so the overlay never goes empty.
     *
     * This prevents the same card from being shown twice in a row while still
     * guaranteeing a result whenever any enabled card exists.
     */
    suspend fun getNextAvailableFlashcard(
        currentTime: Long = System.currentTimeMillis(),
        excludeId: Long = NO_EXCLUDED_CARD
    ): FlashcardEntity? {
        // 1. A ready card other than the one just shown.
        getNextFlashcardForReview(currentTime, excludeId)?.let { return it }

        // 2. Otherwise the soonest-to-be-ready card other than the one just shown.
        getCardWithShortestCooldown(excludeId)?.let { return it }

        // 3. The just-shown card is the only one available: show it rather than nothing.
        return getCardWithShortestCooldown(NO_EXCLUDED_CARD)
    }
    
    @Query("""
        SELECT COUNT(*) FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
    """)
    suspend fun getActiveFlashcardCount(): Int
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE categoryId = :categoryId")
    suspend fun getFlashcardCountByCategory(categoryId: Long): Int

    /** Live flashcard count per category in a single grouped query (categories with 0 cards are absent). */
    @Query("SELECT categoryId AS categoryId, COUNT(*) AS count FROM flashcards GROUP BY categoryId")
    fun getFlashcardCountsPerCategory(): Flow<List<CategoryFlashcardCount>>

    @Insert
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long
    
    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)
    
    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)
    
    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Long)
    
    @Query("DELETE FROM flashcards WHERE categoryId = :categoryId")
    suspend fun deleteFlashcardsByCategoryId(categoryId: Long)
    
    // Statistics reset methods
    @Query("""
        UPDATE flashcards 
        SET correctCount = 0, 
            incorrectCount = 0, 
            hardCount = 0, 
            easinessFactor = 2.5, 
            reviewCount = 0, 
            lastReviewedAt = 0, 
            cooldownUntil = 0,
            updatedAt = :timestamp
        WHERE id = :flashcardId
    """)
    suspend fun resetFlashcardStatistics(flashcardId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE flashcards 
        SET correctCount = 0, 
            incorrectCount = 0, 
            hardCount = 0, 
            easinessFactor = 2.5, 
            reviewCount = 0, 
            lastReviewedAt = 0, 
            cooldownUntil = 0,
            updatedAt = :timestamp
        WHERE categoryId = :categoryId
    """)
    suspend fun resetCategoryStatistics(categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE flashcards 
        SET correctCount = 0, 
            incorrectCount = 0, 
            hardCount = 0, 
            easinessFactor = 2.5, 
            reviewCount = 0, 
            lastReviewedAt = 0, 
            cooldownUntil = 0,
            updatedAt = :timestamp
    """)
    suspend fun resetAllStatistics(timestamp: Long = System.currentTimeMillis())
    
    // Backup-specific methods
    @Query("SELECT * FROM flashcards WHERE question = :question AND answer = :answer LIMIT 1")
    suspend fun getFlashcardByQuestionAndAnswer(question: String, answer: String): FlashcardEntity?
    
    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards()
    
    // Bulk operations for select/deselect all functionality
    @Query("UPDATE flashcards SET isEnabled = 1, updatedAt = :timestamp WHERE categoryId = :categoryId")
    suspend fun enableAllFlashcardsInCategory(categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE flashcards SET isEnabled = 0, updatedAt = :timestamp WHERE categoryId = :categoryId")
    suspend fun disableAllFlashcardsInCategory(categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE categoryId = :categoryId AND isEnabled = 1")
    suspend fun getEnabledFlashcardCountByCategory(categoryId: Long): Int

    // CSV import — batch insert for performance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcardsBatch(flashcards: List<FlashcardEntity>): List<Long>

    // CSV import — duplicate detection across all categories
    @Query("SELECT question, answer FROM flashcards")
    suspend fun getExistingQuestionAnswerPairsAllCategories(): List<QuestionAnswerPair>

    /**
     * Simple data class for duplicate detection.
     */
    data class QuestionAnswerPair(
        val question: String,
        val answer: String
    )

    companion object {
        /**
         * Sentinel for "exclude no card" in the selection queries. Real flashcards
         * use auto-generated ids > 0 and system cards use negative ids, so 0 can
         * never match a stored row.
         */
        const val NO_EXCLUDED_CARD = 0L
    }
}
