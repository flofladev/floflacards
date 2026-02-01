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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.repository.FlashcardRepository
import com.floflacards.app.presentation.component.BulkActionState
import com.floflacards.app.presentation.component.toBulkActionState
import com.floflacards.app.data.source.ImageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardUiState(
    val flashcards: List<FlashcardEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val flashcardToDelete: FlashcardEntity? = null, // Flashcard pending deletion confirmation
    val currentCategoryId: Long = 0L, // Track current category for bulk operations
    val searchQuery: String = "", // Current search query
    // Image state for add/edit screen
    val tempQuestionImagePath: String? = null,
    val tempAnswerImagePath: String? = null
)

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val repository: FlashcardRepository,
    private val imageManager: ImageManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()
    
    fun loadFlashcardsByCategory(categoryId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                currentCategoryId = categoryId
            )
            try {
                repository.getFlashcardsByCategory(categoryId).collect { flashcards ->
                    _uiState.value = _uiState.value.copy(
                        flashcards = flashcards,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun createFlashcard(
        categoryId: Long, 
        question: String, 
        answer: String,
        questionImagePath: String? = null,
        answerImagePath: String? = null
    ) {
        if (question.isBlank() || answer.isBlank()) return
        
        viewModelScope.launch {
            try {
                val flashcard = FlashcardEntity(
                    categoryId = categoryId,
                    question = question.trim(),
                    answer = answer.trim(),
                    questionImagePath = questionImagePath,
                    answerImagePath = answerImagePath
                )
                repository.insertFlashcard(flashcard)
                
                // Clear temporary image state
                clearTempImages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun updateFlashcard(flashcard: FlashcardEntity) {
        viewModelScope.launch {
            try {
                repository.updateFlashcard(flashcard.copy(updatedAt = System.currentTimeMillis()))
                
                // Clear temporary image state
                clearTempImages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun requestDeleteFlashcard(flashcard: FlashcardEntity) {
        _uiState.value = _uiState.value.copy(flashcardToDelete = flashcard)
    }
    
    fun confirmDeleteFlashcard() {
        val flashcardToDelete = _uiState.value.flashcardToDelete ?: return
        viewModelScope.launch {
            try {
                // Delete associated images first
                imageManager.deleteFlashcardImages(
                    flashcardToDelete.questionImagePath,
                    flashcardToDelete.answerImagePath
                )
                
                repository.deleteFlashcard(flashcardToDelete)
                _uiState.value = _uiState.value.copy(flashcardToDelete = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    flashcardToDelete = null
                )
            }
        }
    }
    
    fun cancelDeleteFlashcard() {
        _uiState.value = _uiState.value.copy(flashcardToDelete = null)
    }
    
    fun toggleFlashcardEnabled(flashcard: FlashcardEntity) {
        updateFlashcard(flashcard.copy(
            isEnabled = !flashcard.isEnabled,
            updatedAt = System.currentTimeMillis()
        ))
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    // Bulk operations for select/deselect all functionality
    fun enableAllFlashcardsInCategory() {
        val categoryId = _uiState.value.currentCategoryId
        if (categoryId == 0L) return
        
        viewModelScope.launch {
            try {
                repository.enableAllFlashcardsInCategory(categoryId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun disableAllFlashcardsInCategory() {
        val categoryId = _uiState.value.currentCategoryId
        if (categoryId == 0L) return
        
        viewModelScope.launch {
            try {
                repository.disableAllFlashcardsInCategory(categoryId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun getBulkActionState(): BulkActionState {
        return _uiState.value.flashcards.toBulkActionState { it.isEnabled }
    }
    
    // Search functionality
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query.trim())
    }
    
    /**
     * Returns filtered flashcards based on search query.
     * Searches both question and answer fields with case-insensitive matching.
     */
    fun getFilteredFlashcards(): List<FlashcardEntity> {
        val query = _uiState.value.searchQuery
        return if (query.isBlank()) {
            _uiState.value.flashcards
        } else {
            _uiState.value.flashcards.filter { flashcard ->
                flashcard.question.contains(query, ignoreCase = true) ||
                flashcard.answer.contains(query, ignoreCase = true)
            }
        }
    }
    
    // Image handling methods
    
    /**
     * Save an image from URI for question or answer.
     * Uses temporary flashcard ID (0) until flashcard is created.
     * 
     * @param uri Image URI from picker
     * @param isQuestion True for question image, false for answer
     */
    fun saveImageFromUri(uri: Uri, isQuestion: Boolean) {
        viewModelScope.launch {
            try {
                // Use temporary ID until flashcard is created
                val tempId = System.currentTimeMillis()
                val imagePath = imageManager.saveImage(uri, tempId, isQuestion)
                
                if (isQuestion) {
                    _uiState.value = _uiState.value.copy(tempQuestionImagePath = imagePath)
                } else {
                    _uiState.value = _uiState.value.copy(tempAnswerImagePath = imagePath)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    /**
     * Remove temporary image for question or answer.
     * 
     * @param isQuestion True for question image, false for answer
     */
    fun removeTempImage(isQuestion: Boolean) {
        if (isQuestion) {
            _uiState.value.tempQuestionImagePath?.let { imageManager.deleteImage(it) }
            _uiState.value = _uiState.value.copy(tempQuestionImagePath = null)
        } else {
            _uiState.value.tempAnswerImagePath?.let { imageManager.deleteImage(it) }
            _uiState.value = _uiState.value.copy(tempAnswerImagePath = null)
        }
    }
    
    /**
     * Set existing image path when editing flashcard.
     * 
     * @param questionImagePath Question image path
     * @param answerImagePath Answer image path
     */
    fun setExistingImages(questionImagePath: String?, answerImagePath: String?) {
        _uiState.value = _uiState.value.copy(
            tempQuestionImagePath = questionImagePath,
            tempAnswerImagePath = answerImagePath
        )
    }
    
    /**
     * Clear temporary image state.
     * Called after successful create/update or when navigating away.
     */
    private fun clearTempImages() {
        _uiState.value = _uiState.value.copy(
            tempQuestionImagePath = null,
            tempAnswerImagePath = null
        )
    }
}
