package com.floflacards.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.repository.FlashcardRepository
import com.floflacards.app.presentation.component.BulkActionState
import com.floflacards.app.presentation.component.toBulkActionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val categoryToDelete: CategoryEntity? = null, // Category pending deletion confirmation
    val categoryFlashcardCount: Int = 0, // Number of flashcards in category to delete
    val searchQuery: String = "" // Current search query
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: FlashcardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getAllCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(
                        categories = categories,
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
    
    fun createCategory(name: String) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            try {
                val category = CategoryEntity(name = name.trim())
                repository.insertCategory(category)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                repository.updateCategory(category.copy(updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun requestDeleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                // Get flashcard count for confirmation dialog
                val flashcardCount = repository.getFlashcardCountByCategory(category.id)
                
                _uiState.value = _uiState.value.copy(
                    categoryToDelete = category,
                    categoryFlashcardCount = flashcardCount
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun confirmDeleteCategory() {
        val categoryToDelete = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            try {
                repository.deleteCategory(categoryToDelete)
                _uiState.value = _uiState.value.copy(
                    categoryToDelete = null,
                    categoryFlashcardCount = 0
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    categoryToDelete = null,
                    categoryFlashcardCount = 0
                )
            }
        }
    }
    
    fun cancelDeleteCategory() {
        _uiState.value = _uiState.value.copy(
            categoryToDelete = null,
            categoryFlashcardCount = 0
        )
    }
    
    fun toggleCategoryEnabled(category: CategoryEntity) {
        updateCategory(category.copy(
            isEnabled = !category.isEnabled,
            updatedAt = System.currentTimeMillis()
        ))
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    // Bulk operations for select/deselect all functionality
    fun enableAllCategories() {
        viewModelScope.launch {
            try {
                repository.enableAllCategories()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun disableAllCategories() {
        viewModelScope.launch {
            try {
                repository.disableAllCategories()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun getBulkActionState(): BulkActionState {
        return _uiState.value.categories.toBulkActionState { it.isEnabled }
    }
    
    // Search functionality
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query.trim())
    }
    
    /**
     * Returns filtered categories based on search query.
     * Uses case-insensitive search on category name.
     */
    fun getFilteredCategories(): List<CategoryEntity> {
        val query = _uiState.value.searchQuery
        return if (query.isBlank()) {
            _uiState.value.categories
        } else {
            _uiState.value.categories.filter { category ->
                category.name.contains(query, ignoreCase = true)
            }
        }
    }
}
