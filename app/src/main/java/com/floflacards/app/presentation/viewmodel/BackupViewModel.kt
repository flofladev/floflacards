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

package com.floflacards.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floflacards.app.data.backup.BackupInfo
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.domain.usecase.backup.CreateBackupUseCase
import com.floflacards.app.domain.usecase.backup.GetBackupInfoUseCase
import com.floflacards.app.domain.usecase.backup.RestoreBackupUseCase
import com.floflacards.app.service.LearningServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Backs the Settings -> Data section: backup folder selection, the automatic
 * backup toggle, manual backup, and restore. The heavy lifting stays in the
 * backup use cases; this only orchestrates them and exposes UI state.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupPreferences: BackupPreferences,
    private val createBackupUseCase: CreateBackupUseCase,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val learningServiceManager: LearningServiceManager,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSettingsUiState())
    val uiState: StateFlow<DataSettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * Re-reads folder configuration and the backup file's info. The "last backup"
     * details come from the actual file, not a stored timestamp, so a backup synced
     * into the folder from another device shows up correctly too.
     */
    fun refresh() {
        viewModelScope.launch {
            val hasFolder = backupPreferences.hasSafFolderConfigured()
            val folderName = if (hasFolder) resolveFolderName() else null
            val info = if (hasFolder) {
                try {
                    getBackupInfoUseCase()
                } catch (e: Exception) {
                    BackupInfo(exists = false, filePath = "")
                }
            } else {
                BackupInfo(exists = false, filePath = "")
            }
            _uiState.value = _uiState.value.copy(
                hasFolderConfigured = hasFolder,
                folderName = folderName,
                autoBackupEnabled = backupPreferences.isAutoBackupEnabled(),
                backupInfo = info
            )
        }
    }

    /** Stores the picked SAF tree URI (persistable permission is taken by the caller). */
    fun onFolderPicked(treeUri: String) {
        backupPreferences.setSafTreeUri(treeUri)
        refresh()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        backupPreferences.setAutoBackupEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoBackupEnabled = enabled)
    }

    /**
     * Writes a backup immediately. Mirrors BackupWorker's bookkeeping (so the
     * automatic path knows the data is already backed up) and its empty-database
     * protection: one tap on a fresh install must never overwrite a real backup
     * with an empty one.
     */
    fun backupNow() {
        if (_uiState.value.isBackingUp || _uiState.value.isRestoring) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true)

            if (categoryDao.getCategoryCount() == 0) {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    lastActionResult = DataActionResult.BACKUP_BLOCKED_EMPTY
                )
                return@launch
            }

            val versionAtStart = backupPreferences.getDataVersion()
            val result = createBackupUseCase()
            result.onSuccess {
                backupPreferences.setLastBackedUpVersion(versionAtStart)
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())
            }
            _uiState.value = _uiState.value.copy(
                isBackingUp = false,
                lastActionResult = if (result.isSuccess) {
                    DataActionResult.BACKUP_SUCCESS
                } else {
                    DataActionResult.BACKUP_FAILED
                }
            )
            refresh()
        }
    }

    /**
     * Restores the backup from the configured folder, replacing all current data.
     * Learning is stopped first so no session keeps rating cards that are being
     * wiped underneath it. The restore itself is transactional: on failure the
     * current data is left untouched.
     */
    fun restoreBackup() {
        if (_uiState.value.isBackingUp || _uiState.value.isRestoring) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)

            learningServiceManager.stopLearningService()

            val restoreResult = restoreBackupUseCase().getOrNull()
            _uiState.value = if (restoreResult?.success == true) {
                _uiState.value.copy(
                    isRestoring = false,
                    lastActionResult = DataActionResult.RESTORE_SUCCESS,
                    restoredCategories = restoreResult.categoriesRestored,
                    restoredFlashcards = restoreResult.flashcardsRestored
                )
            } else {
                _uiState.value.copy(
                    isRestoring = false,
                    lastActionResult = DataActionResult.RESTORE_FAILED
                )
            }
            refresh()
        }
    }

    /** Dismisses the result dialog. */
    fun clearActionResult() {
        _uiState.value = _uiState.value.copy(lastActionResult = null)
    }

    /**
     * Human-readable location of the configured backup folder. For the system
     * storage provider the tree URI's document id is a real path
     * ("primary:Documents/FloFla" -> "Documents/FloFla", SD cards keep their
     * volume id as prefix), which disambiguates same-named folders. Cloud
     * providers use opaque ids with no path, so the folder name is shown there.
     */
    private suspend fun resolveFolderName(): String? = withContext(Dispatchers.IO) {
        try {
            val uriString = backupPreferences.getSafTreeUri() ?: return@withContext null
            val uri = Uri.parse(uriString)

            if (uri.authority == "com.android.externalstorage.documents") {
                val docId = DocumentsContract.getTreeDocumentId(uri) // volume:relative/path
                val volume = docId.substringBefore(':')
                val relativePath = docId.substringAfter(':', "")
                val parts = listOfNotNull(
                    volume.takeUnless { it.equals("primary", ignoreCase = true) },
                    relativePath.takeIf { it.isNotEmpty() }
                )
                if (parts.isNotEmpty()) return@withContext parts.joinToString("/")
            }

            DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }
}

/** What the last manual action ended as; drives the one-off result dialog. */
enum class DataActionResult {
    BACKUP_SUCCESS,
    BACKUP_FAILED,
    BACKUP_BLOCKED_EMPTY,
    RESTORE_SUCCESS,
    RESTORE_FAILED
}

/** UI state for the Settings -> Data section. */
data class DataSettingsUiState(
    val hasFolderConfigured: Boolean = false,
    val folderName: String? = null,
    val autoBackupEnabled: Boolean = true,
    val backupInfo: BackupInfo = BackupInfo(exists = false, filePath = ""),
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val lastActionResult: DataActionResult? = null,
    val restoredCategories: Int = 0,
    val restoredFlashcards: Int = 0
)
