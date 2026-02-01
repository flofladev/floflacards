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

package com.floflacards.app.domain.model

/**
 * Simple streak data model following KISS principle.
 * Tracks user's learning streak based on flashcard viewing activity.
 * Uses timestamps for API compatibility (min API 24).
 */
data class StreakData(
    val currentStreak: Int = 0,
    val lastActivityTimestamp: Long = 0,
    val highestStreak: Int = 0
) {
    
    /**
     * Updates streak based on today's activity.
     * Simple logic: increment if consecutive day, reset if gap, no change if same day.
     */
    fun updateStreakOnActivity(todayTimestamp: Long = System.currentTimeMillis()): StreakData {
        val oneDayMs = 24 * 60 * 60 * 1000L
        val todayDayNumber = todayTimestamp / oneDayMs
        val lastActivityDayNumber = if (lastActivityTimestamp > 0) lastActivityTimestamp / oneDayMs else -1
        
        return when {
            lastActivityTimestamp == 0L -> {
                // First time ever using the app
                StreakData(
                    currentStreak = 1,
                    lastActivityTimestamp = todayTimestamp,
                    highestStreak = 1
                )
            }
            todayDayNumber == lastActivityDayNumber -> {
                // Same day, no change to streak
                this
            }
            todayDayNumber == lastActivityDayNumber + 1 -> {
                // Next consecutive day - increment streak
                val newStreak = currentStreak + 1
                copy(
                    currentStreak = newStreak,
                    lastActivityTimestamp = todayTimestamp,
                    highestStreak = maxOf(highestStreak, newStreak)
                )
            }
            else -> {
                // Gap detected - reset streak to 1
                copy(
                    currentStreak = 1,
                    lastActivityTimestamp = todayTimestamp,
                    highestStreak = maxOf(highestStreak, 1)
                )
            }
        }
    }
    
    /**
     * Gets the current valid streak with real-time validation.
     * Zero performance impact: simple timestamp comparison, no I/O, no background tasks.
     * Shows 0 if streak is broken (gap > 1 day), maintaining user trust and motivation.
     */
    fun getCurrentValidStreak(currentTimestamp: Long = System.currentTimeMillis()): Int {
        if (lastActivityTimestamp == 0L || currentStreak == 0) return 0
        
        val oneDayMs = 24 * 60 * 60 * 1000L
        val currentDayNumber = currentTimestamp / oneDayMs
        val lastActivityDayNumber = lastActivityTimestamp / oneDayMs
        val daysSinceLastActivity = currentDayNumber - lastActivityDayNumber
        
        return when {
            daysSinceLastActivity <= 1 -> currentStreak  // Same day or next day - valid streak
            else -> 0  // Gap detected - show 0 until user resumes activity
        }
    }
}
