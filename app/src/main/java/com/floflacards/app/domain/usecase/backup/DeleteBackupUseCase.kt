package com.floflacards.app.domain.usecase.backup

import com.floflacards.app.data.repository.BackupRepository
import javax.inject.Inject

/**
 * Use case for deleting backups.
 * Follows single responsibility principle.
 */
class DeleteBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return backupRepository.deleteBackup()
    }
}
