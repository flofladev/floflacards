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
