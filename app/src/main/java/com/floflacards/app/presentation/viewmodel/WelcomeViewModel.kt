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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floflacards.app.util.PermissionHelper
import com.floflacards.app.data.backup.BackupInfo
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.domain.usecase.backup.GetBackupInfoUseCase
import com.floflacards.app.domain.usecase.backup.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the redesigned Welcome flow.
 *
 * The onboarding is grouped into four screens by intent instead of one-per-permission:
 *   WELCOME (intro + privacy + language) -> PERMISSIONS (overlay + battery) ->
 *   BACKUP (folder + inline restore) -> COMPLETED.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val permissionHelper: PermissionHelper,
    private val settingsManager: SettingsRepository,
    private val backupPreferences: BackupPreferences,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
    }

    /**
     * Refreshes permission/folder state. Called on resume and after SAF folder selection.
     * Also re-checks backup availability whenever a folder is configured, so the BACKUP
     * screen can reveal the restore option as soon as a folder containing a backup is chosen.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val isBatteryOptDisabled = permissionHelper.isBatteryOptimizationDisabled()

            // Track when the user successfully disables battery optimization.
            if (isBatteryOptDisabled && !settingsManager.hasBatteryOptimizationEverBeenDisabled()) {
                settingsManager.setBatteryOptimizationEverDisabled(true)
            }

            val hasFolder = backupPreferences.hasSafFolderConfigured()
            _uiState.value = _uiState.value.copy(
                hasOverlayPermission = permissionHelper.hasOverlayPermission(),
                isBatteryOptimizationDisabled = isBatteryOptDisabled,
                hasBackupFolderConfigured = hasFolder,
                isRefreshing = false
            )

            if (hasFolder) checkBackupAvailability()
        }
    }

    /** Advances to the next onboarding screen. */
    fun nextStep() {
        val next = when (_uiState.value.currentStep) {
            WelcomeStep.WELCOME -> WelcomeStep.PERMISSIONS
            WelcomeStep.PERMISSIONS -> WelcomeStep.BACKUP
            WelcomeStep.BACKUP -> WelcomeStep.COMPLETED
            WelcomeStep.COMPLETED -> WelcomeStep.COMPLETED
        }
        _uiState.value = _uiState.value.copy(currentStep = next)
    }

    /**
     * Leaves the PERMISSIONS screen. Overlay is required (the UI gates Continue on it);
     * battery optimization is optional, so if it wasn't disabled we record it as skipped
     * to avoid nagging the user later, then advance.
     */
    fun proceedFromPermissions() {
        if (!_uiState.value.isBatteryOptimizationDisabled) {
            settingsManager.setBatteryOptimizationSkipped(true)
        }
        nextStep()
    }

    /** Requests the system battery-optimization-disable dialog. */
    fun requestBatteryOptimizationDisable() {
        permissionHelper.requestBatteryOptimizationDisable()
    }

    /** Looks up whether the configured backup folder contains a restorable backup. */
    private fun checkBackupAvailability() {
        viewModelScope.launch {
            try {
                val info = getBackupInfoUseCase()
                _uiState.value = _uiState.value.copy(
                    backupInfo = info,
                    hasBackupAvailable = info.exists
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    backupInfo = BackupInfo(exists = false, filePath = ""),
                    hasBackupAvailable = false
                )
            }
        }
    }

    /** Restores the detected backup, then moves to the completion screen. */
    fun restoreBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            try {
                restoreBackupUseCase()
            } catch (e: Exception) {
                // Keep the UX simple: proceed even if restore fails.
            }
            _uiState.value = _uiState.value.copy(isRestoring = false)
            completeWelcomeFlow()
        }
    }

    /** Proceeds without restoring (fresh start or no backup found). */
    fun startFresh() {
        completeWelcomeFlow()
    }

    private fun completeWelcomeFlow() {
        _uiState.value = _uiState.value.copy(currentStep = WelcomeStep.COMPLETED)
    }

    /** Stores the SAF tree URI and refreshes (which re-checks for a backup). */
    fun handleSafFolderSelected(treeUri: String) {
        backupPreferences.setSafTreeUri(treeUri)
        refreshPermissions()
    }
}

/**
 * UI state for the welcome screen.
 */
data class WelcomeUiState(
    val currentStep: WelcomeStep = WelcomeStep.WELCOME,
    val hasOverlayPermission: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val hasBackupFolderConfigured: Boolean = false,
    val hasBackupAvailable: Boolean = false,
    val backupInfo: BackupInfo = BackupInfo(exists = false, filePath = ""),
    val isRestoring: Boolean = false,
    val isRefreshing: Boolean = false
)

/**
 * Welcome flow steps (grouped by intent).
 */
enum class WelcomeStep {
    WELCOME,
    PERMISSIONS,
    BACKUP,
    COMPLETED
}
