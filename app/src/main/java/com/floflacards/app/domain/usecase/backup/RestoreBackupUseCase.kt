package com.floflacards.app.domain.usecase.backup

import com.floflacards.app.data.backup.RestoreResult
import com.floflacards.app.data.repository.BackupRepository
import javax.inject.Inject

/**
 * Use case for restoring backups.
 * Follows single responsibility principle.
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Result<RestoreResult> {
        return backupRepository.restoreBackup()
    }
}
