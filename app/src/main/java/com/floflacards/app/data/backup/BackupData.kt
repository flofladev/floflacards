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

package com.floflacards.app.data.backup

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Main backup data structure containing all app data.
 * Uses JSON serialization with UUIDs for data integrity.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val backupId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val categories: List<CategoryBackup>,
    val flashcards: List<FlashcardBackup>,
    val streakData: StreakBackup? = null, // Optional for backward compatibility
    val metadata: BackupMetadata
)

/**
 * Category backup data with UUID.
 */
@Serializable
data class CategoryBackup(
    val id: Long,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Flashcard backup data with UUID and complete statistics.
 */
@Serializable
data class FlashcardBackup(
    val id: Long,
    val uuid: String = UUID.randomUUID().toString(),
    val categoryId: Long,
    val categoryUuid: String, // Reference to category UUID
    val question: String,
    val answer: String,
    val questionImagePath: String? = null, // Path to question image (nullable)
    val answerImagePath: String? = null, // Path to answer image (nullable)
    val isEnabled: Boolean,
    val correctCount: Int,
    val incorrectCount: Int,
    val hardCount: Int,
    val easinessFactor: Float,
    val reviewCount: Int,
    val lastReviewedAt: Long,
    val cooldownUntil: Long,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Streak backup data for preserving user progress.
 */
@Serializable
data class StreakBackup(
    val currentStreak: Int,
    val highestStreak: Int,
    val lastActivityTimestamp: Long
)

/**
 * Backup metadata for validation and statistics.
 */
@Serializable
data class BackupMetadata(
    val appVersion: String = "1.0.0",
    val totalCategories: Int,
    val totalFlashcards: Int,
    val totalReviews: Int,
    val deviceInfo: String = android.os.Build.MODEL,
    val backupSource: String = "automatic"
)

/**
 * Result of backup restore operation.
 */
data class RestoreResult(
    val success: Boolean,
    val categoriesRestored: Int = 0,
    val flashcardsRestored: Int = 0,
    val error: String? = null
)

/**
 * Backup file information for UI display.
 */
data class BackupInfo(
    val exists: Boolean,
    val filePath: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val categoryCount: Int = 0,
    val flashcardCount: Int = 0,
    val totalReviews: Int = 0,
    val fileSize: Long = 0
)
