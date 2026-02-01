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

package com.floflacards.app.data.source

import android.content.Context
import android.content.SharedPreferences
import com.floflacards.app.domain.model.StreakData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages streak-related preferences following SOLID principles.
 * Single responsibility: Handle streak data persistence.
 * Uses timestamps for API compatibility (min API 24).
 */
@Singleton
class StreakPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "streak_preferences"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LAST_ACTIVITY_TIMESTAMP = "last_activity_timestamp"
        private const val KEY_HIGHEST_STREAK = "highest_streak"
    }

    /**
     * Get current streak data.
     */
    fun getStreakData(): StreakData {
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val highestStreak = prefs.getInt(KEY_HIGHEST_STREAK, 0)
        val lastActivityTimestamp = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, 0L)
        
        return StreakData(
            currentStreak = currentStreak,
            lastActivityTimestamp = lastActivityTimestamp,
            highestStreak = highestStreak
        )
    }

    /**
     * Save streak data.
     */
    fun saveStreakData(streakData: StreakData) {
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, streakData.currentStreak)
            .putInt(KEY_HIGHEST_STREAK, streakData.highestStreak)
            .putLong(KEY_LAST_ACTIVITY_TIMESTAMP, streakData.lastActivityTimestamp)
            .apply()
    }

    /**
     * Update streak based on flashcard activity today.
     * Returns the updated streak data.
     */
    fun updateStreakOnActivity(todayTimestamp: Long = System.currentTimeMillis()): StreakData {
        val currentData = getStreakData()
        val updatedData = currentData.updateStreakOnActivity(todayTimestamp)
        saveStreakData(updatedData)
        return updatedData
    }

    /**
     * Reset streak data (for testing or user request).
     */
    fun resetStreak() {
        prefs.edit()
            .remove(KEY_CURRENT_STREAK)
            .remove(KEY_LAST_ACTIVITY_TIMESTAMP)
            .remove(KEY_HIGHEST_STREAK)
            .apply()
    }
}
