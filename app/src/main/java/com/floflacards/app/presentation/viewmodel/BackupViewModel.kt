package com.floflacards.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.floflacards.app.domain.usecase.backup.CreateBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for backup operations following MVVM pattern.
 * Manages backup state and coordinates with use cases.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    
    init {
        // Backup check is now handled in Welcome Screen
        // No automatic backup dialog in main app - follows DRY principle
    }



    /**
     * Creates manual backup.
     */
    fun createManualBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = createBackupUseCase()
                result.fold(
                    onSuccess = { filePath ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        // Success - backup created successfully
                        // UI feedback would be handled by the calling component
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to create backup: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Backup failed: ${e.message}"
                )
            }
        }
    }



    /**
     * Clears error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for backup operations.
 * Cleaned up to remove unused dialog properties - dialogs now handled in Welcome Screen.
 */
data class BackupUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
