package com.floflacards.app.domain.usecase

import com.floflacards.app.data.source.StreakPreferences
import com.floflacards.app.domain.model.StreakData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple streak use case following SOLID principles.
 * Single Responsibility: Handle streak business logic.
 * Replaces complex StreakCalculator with simple, user-friendly streak tracking.
 * Uses timestamps for API compatibility (min API 24).
 */
@Singleton
class SimpleStreakUseCase @Inject constructor(
    private val streakPreferences: StreakPreferences
) {
    
    /**
     * Get current streak data with real-time validation.
     * Ensures displayed streak reflects current reality, not stale data.
     * Zero performance impact - just timestamp comparison.
     */
    fun getCurrentStreakData(): StreakData {
        val rawData = streakPreferences.getStreakData()
        // Return validated data with real-time streak calculation
        return rawData.copy(
            currentStreak = rawData.getCurrentValidStreak()
        )
    }
    
    /**
     * Record flashcard activity and update streak accordingly.
     * This should be called whenever a flashcard is shown/viewed by the user.
     */
    fun recordFlashcardActivity(todayTimestamp: Long = System.currentTimeMillis()): StreakData {
        return streakPreferences.updateStreakOnActivity(todayTimestamp)
    }
    
    /**
     * Reset streak data (for user request or testing).
     */
    fun resetStreak() {
        streakPreferences.resetStreak()
    }
}
