package com.floflacards.app.domain.usecase.backup

import com.floflacards.app.data.repository.BackupRepository
import javax.inject.Inject

/**
 * Use case for creating backups.
 * Follows single responsibility principle.
 */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Result<String> {
        return backupRepository.createBackup()
    }
}
