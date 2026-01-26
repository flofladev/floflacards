package com.floflacards.app.domain.model

/**
 * Configuration for the SM-2 Spaced Repetition System.
 * Adapted to work with user-defined intervals while maintaining SM-2 principles.
 */
data class Sm2Config(
    // SM-2 Easiness Factor constraints
    val minEasinessFactor: Float = 1.3f,
    val maxEasinessFactor: Float = 2.5f,
    val defaultEasinessFactor: Float = 2.5f,
    
    // SM-2 Quality response adjustments
    val easinessFactorAdjustment: Float = 0.1f,
    val qualityPenalty: Float = 0.08f,
    val qualityPenaltySquared: Float = 0.02f,
    
    // Initial intervals (in user interval units)
    val firstReviewInterval: Int = 1,   // First review after 1 interval
    val secondReviewInterval: Int = 6,  // Second review after 6 intervals
    
    // Failed card intervals with progressive penalty
    val baseFailedCardInterval: Int = 2,  // Base cooldown for failed cards
    val maxFailedCardInterval: Int = 10,  // Maximum cooldown for repeatedly failed cards
) {
    companion object {
        fun getDefault() = Sm2Config()
    }
}

/**
 * Represents the user's rating of a flashcard
 */
enum class FlashcardRating(val displayName: String) {
    WRONG("Wrong"),
    HARD("Hard"), 
    GOOD("Good"),
    CLOSED("Closed") // When user closes without rating
}

/**
 * Result of SM-2 algorithm calculation
 */
data class Sm2Result(
    val cooldownUntilTimestamp: Long,    // When card becomes available again
    val newEasinessFactor: Float,        // Updated easiness factor
    val newReviewCount: Int,             // Updated review count
    val shouldUpdateStats: Boolean = true  // Whether to update correctCount/incorrectCount
)
