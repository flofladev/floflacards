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

package com.floflacards.app.data.repository

import com.floflacards.app.data.backup.BackupInfo
import com.floflacards.app.data.backup.BackupManager
import com.floflacards.app.data.backup.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for backup operations following clean architecture.
 * Provides abstraction between domain and data layers.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val backupManager: BackupManager
) {
    
    /**
     * Checks if a backup file exists.
     */
    suspend fun hasExistingBackup(): Boolean = withContext(Dispatchers.IO) {
        backupManager.hasExistingBackup()
    }
    
    /**
     * Gets backup file information for UI display.
     */
    suspend fun getBackupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        backupManager.getBackupInfo()
    }
    
    /**
     * Creates or updates backup file.
     * Called automatically on flashcard operations.
     */
    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        backupManager.createBackup()
    }
    
    /**
     * Restores data from backup file.
     */
    suspend fun restoreBackup(): Result<RestoreResult> = withContext(Dispatchers.IO) {
        backupManager.restoreBackup()
    }
    
    /**
     * Deletes the backup file.
     */
    suspend fun deleteBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        backupManager.deleteBackup()
    }
}
