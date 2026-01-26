package com.floflacards.app.domain.usecase.backup

import com.floflacards.app.data.backup.BackupInfo
import com.floflacards.app.data.repository.BackupRepository
import javax.inject.Inject

/**
 * Use case for getting backup information.
 * Follows single responsibility principle.
 */
class GetBackupInfoUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): BackupInfo {
        return backupRepository.getBackupInfo()
    }
}
