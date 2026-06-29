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

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floflacards.app.data.repository.FlashcardRepository
import com.floflacards.app.domain.usecase.csv.ExportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for CSV export operations.
 * Manages export execution and result state.
 */
@HiltViewModel
class CsvExportViewModel @Inject constructor(
    private val exportCsvUseCase: ExportCsvUseCase,
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CsvExportUiState())
    val uiState: StateFlow<CsvExportUiState> = _uiState.asStateFlow()

    /**
     * Loads the category's display name from its id. The route only carries the id, so the name
     * is resolved here from the single source of truth (immune to special characters in the name).
     */
    fun loadCategory(categoryId: Long) {
        viewModelScope.launch {
            val name = repository.getCategoryById(categoryId)?.name ?: ""
            _uiState.value = _uiState.value.copy(categoryName = name)
        }
    }

    /**
     * Exports flashcards from the given category to a URI.
     */
    fun exportCategory(
        categoryId: Long,
        contentResolver: ContentResolver,
        uri: Uri
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = exportCsvUseCase(categoryId, outputStream)
                    result.fold(
                        onSuccess = { count ->
                            _uiState.value = _uiState.value.copy(isLoading = false, exportedCount = count, exportComplete = true)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(isLoading = false, error = "Export failed: ${error.message}")
                        }
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Cannot open output stream")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Export failed: ${e.message}")
            }
        }
    }

    /**
     * Resets the export state.
     */
    fun reset() {
        _uiState.value = CsvExportUiState()
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for CSV export.
 */
data class CsvExportUiState(
    val isLoading: Boolean = false,
    val exportedCount: Int = 0,
    val exportComplete: Boolean = false,
    val error: String? = null,
    // Category display name; null until loaded (the route waits for it before suggesting a filename).
    val categoryName: String? = null
)
