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
