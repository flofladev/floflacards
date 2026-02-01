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
