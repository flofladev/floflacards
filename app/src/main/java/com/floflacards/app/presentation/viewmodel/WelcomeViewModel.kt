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
 * ViewModel for Welcome Screen following MVVM architecture.
 * Manages permission states and welcome flow progression.
 * Follows SRP by handling only welcome-related logic.
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
        // Don't check backup availability here - only when user reaches backup step
        // This ensures storage permission is granted first
    }

    /**
     * Refreshes all permission states and SAF folder configuration.
     * Called when returning from system settings or SAF folder selection.
     * Tracks when user successfully disables battery optimization.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val isBatteryOptDisabled = permissionHelper.isBatteryOptimizationDisabled()
            
            // Track when user successfully disables battery optimization
            if (isBatteryOptDisabled && !settingsManager.hasBatteryOptimizationEverBeenDisabled()) {
                settingsManager.setBatteryOptimizationEverDisabled(true)
            }
            
            _uiState.value = currentState.copy(
                hasOverlayPermission = permissionHelper.hasOverlayPermission(),
                isBatteryOptimizationDisabled = isBatteryOptDisabled,
                hasBackupFolderConfigured = backupPreferences.hasSafFolderConfigured(),
                isRefreshing = false
            )
            updateCurrentStep()
        }
    }

    /**
     * Moves to the next step in the welcome flow.
     * Automatically skips backup step if no backup is available (KISS principle).
     */
    fun nextStep() {
        val currentState = _uiState.value
        val nextStep = when (currentState.currentStep) {
            WelcomeStep.INTRODUCTION -> WelcomeStep.PRIVACY_OFFLINE
            WelcomeStep.PRIVACY_OFFLINE -> WelcomeStep.BACKUP_FOLDER
            WelcomeStep.BACKUP_FOLDER -> WelcomeStep.OVERLAY_PERMISSION
            WelcomeStep.OVERLAY_PERMISSION -> WelcomeStep.BATTERY_OPTIMIZATION
            WelcomeStep.BATTERY_OPTIMIZATION -> {
                // Check backup availability and handle step transition asynchronously
                checkBackupAvailabilityAndAdvance()
                // Return current step for now, actual transition happens in checkBackupAvailabilityAndAdvance
                currentState.currentStep
            }
            WelcomeStep.BACKUP_CHECK -> WelcomeStep.COMPLETED
            WelcomeStep.COMPLETED -> WelcomeStep.COMPLETED
        }
        
        _uiState.value = currentState.copy(currentStep = nextStep)
    }

    /**
     * Updates the current step based on permission states.
     * Automatically advances if permissions are already granted.
     */
    private fun updateCurrentStep() {
        val currentState = _uiState.value
        
        // Auto-advance through completed steps
        when (currentState.currentStep) {
            WelcomeStep.OVERLAY_PERMISSION -> {
                if (currentState.hasOverlayPermission) {
                    nextStep()
                }
            }
            WelcomeStep.BATTERY_OPTIMIZATION -> {
                if (currentState.isBatteryOptimizationDisabled) {
                    nextStep()
                }
            }
            else -> { /* No auto-advance needed */ }
        }
    }

    /**
     * Checks backup availability and advances to appropriate step.
     * Auto-skips backup step if no backup found (KISS principle).
     */
    private fun checkBackupAvailabilityAndAdvance() {
        viewModelScope.launch {
            try {
                val backupInfo = getBackupInfoUseCase()
                val hasBackup = backupInfo.exists
                
                _uiState.value = _uiState.value.copy(
                    backupInfo = backupInfo,
                    hasBackupAvailable = hasBackup
                )
                
                if (hasBackup) {
                    _uiState.value = _uiState.value.copy(currentStep = WelcomeStep.BACKUP_CHECK)
                } else {
                    completeWelcomeFlow()
                }
            } catch (e: Exception) {
                // If backup check fails, assume no backup available and skip to completion
                _uiState.value = _uiState.value.copy(
                    backupInfo = BackupInfo(exists = false, filePath = ""),
                    hasBackupAvailable = false
                )
                completeWelcomeFlow()
            }
        }
    }

    /**
     * Completes the welcome flow by advancing to the final step.
     * Follows DRY principle - single point for completion logic.
     */
    private fun completeWelcomeFlow() {
        _uiState.value = _uiState.value.copy(currentStep = WelcomeStep.COMPLETED)
    }


    /**
     * Requests battery optimization disable.
     */
    fun requestBatteryOptimizationDisable() {
        permissionHelper.requestBatteryOptimizationDisable()
    }
    
    /**
     * Skips battery optimization setup during welcome flow.
     * Persists user choice to prevent showing dialog later.
     * Follows SRP by handling only skip logic.
     */
    fun skipBatteryOptimization() {
        settingsManager.setBatteryOptimizationSkipped(true)
        nextStep() // Proceed to next welcome step
    }

    /**
     * Restores backup if available.
     * Follows SRP - handles only backup restoration, delegates flow completion.
     */
    fun restoreBackup() {
        viewModelScope.launch {
            try {
                val result = restoreBackupUseCase()
                result.fold(
                    onSuccess = { restoreResult ->
                        // After successful restore, complete the welcome flow
                        completeWelcomeFlow()
                    },
                    onFailure = { error ->
                        // On failure, still complete flow (keeps UX simple)
                        completeWelcomeFlow()
                    }
                )
            } catch (e: Exception) {
                // On exception, still complete flow
                completeWelcomeFlow()
            }
        }
    }

    /**
     * Handles SAF tree URI result from folder selection.
     * Stores the URI and updates the UI state.
     */
    fun handleSafFolderSelected(treeUri: String) {
        backupPreferences.setSafTreeUri(treeUri)
        refreshPermissions()
    }

    /**
     * Checks if all required permissions are granted.
     * Respects user's choice to skip battery optimization and tracks disable intent.
     */
    fun areAllPermissionsGranted(): Boolean {
        val state = _uiState.value
        val batteryOptSkipped = settingsManager.isBatteryOptimizationSkipped()
        val batteryOptEverDisabled = settingsManager.hasBatteryOptimizationEverBeenDisabled()
        
        val hasRequiredPermissions = state.hasOverlayPermission
        
        // Battery optimization logic:
        // 1. If user skipped: always consider requirement met
        // 2. If user disabled but system re-enabled: consider requirement met (main app handles contextual dialogs)
        // 3. If fresh install and not disabled: requirement not met
        val batteryOptRequirementMet = state.isBatteryOptimizationDisabled || 
                                       batteryOptSkipped || 
                                       batteryOptEverDisabled
        
        return hasRequiredPermissions && batteryOptRequirementMet
    }

    /**
     * Checks if the welcome flow can be completed.
     */
    fun canCompleteWelcome(): Boolean {
        return areAllPermissionsGranted() && _uiState.value.currentStep == WelcomeStep.COMPLETED
    }
}

/**
 * UI state for the welcome screen.
 * Follows data encapsulation principles.
 */
data class WelcomeUiState(
    val currentStep: WelcomeStep = WelcomeStep.INTRODUCTION,
    val hasOverlayPermission: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val hasBackupFolderConfigured: Boolean = false,
    val hasBackupAvailable: Boolean = false,
    val backupInfo: BackupInfo = BackupInfo(exists = false, filePath = ""),
    val isRefreshing: Boolean = false
)

/**
 * Welcome flow steps.
 * Defines the mandatory progression through the onboarding.
 */
enum class WelcomeStep {
    INTRODUCTION,
    PRIVACY_OFFLINE,
    BACKUP_FOLDER,
    OVERLAY_PERMISSION,
    BATTERY_OPTIMIZATION,
    BACKUP_CHECK,
    COMPLETED
}
