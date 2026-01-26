package com.floflacards.app.service

import android.util.Log
import androidx.compose.runtime.*
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.domain.model.FlashcardRating
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.data.source.FlashcardUiPreferences
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.presentation.component.FlashcardContainer
import com.floflacards.app.presentation.component.flashcard.EmptyStateFlashcardContainer
import androidx.compose.runtime.collectAsState

/**
 * Provides UI composition logic for overlay service.
 * Follows Single Responsibility Principle by separating UI logic from service management.
 */
class OverlayComponents(
    private val categoryDao: CategoryDao,
    private val flashcardUiPreferences: FlashcardUiPreferences,
    private val settingsManager: SettingsRepository
) {
    companion object {
        private const val TAG = "OverlayComponents"
    }
    
    /**
     * Creates the main overlay UI composition with flashcard content.
     * Follows SOLID principles with clean separation of concerns.
     */
    @Composable
    fun OverlayContent(
        flashcard: FlashcardEntity,
        onPositionChange: (Int, Int) -> Unit,
        onSizeChange: (Int, Int) -> Unit,
        onRating: (FlashcardRating) -> Unit,
        onClose: () -> Unit,
        onManageCards: () -> Unit = { } // Default empty implementation for regular flashcards
    ) {
        // Get category information
        var category by remember { mutableStateOf<CategoryEntity?>(null) }
        
        // Reactive UI state that updates when preferences change
        var currentUiState by remember { mutableStateOf(flashcardUiPreferences.getFlashcardUiState()) }
        
        // Get current flashcard theme from settings - reactive to theme changes
        val currentFlashcardTheme by settingsManager.flashcardTheme.collectAsState()
        
        LaunchedEffect(flashcard.categoryId) {
            try {
                category = if (flashcard.categoryId == -1L) {
                    // Demo flashcard - create demo category
                    CategoryEntity(id = -1L, name = "Demo Category")
                } else {
                    categoryDao.getCategoryById(flashcard.categoryId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get category", e)
            }
        }
        
        // Detect empty state flashcard and use appropriate container
        if (flashcard.id == -2L) {
            // Empty state flashcard - use specialized container
            EmptyStateFlashcardContainer(
                flashcard = flashcard,
                uiState = currentUiState,
                theme = currentFlashcardTheme,
                onPositionChange = onPositionChange,
                onSizeChange = onSizeChange,
                onModeSelected = { mode ->
                    // Enhanced mode selection - cleaner than old cycling logic
                    flashcardUiPreferences.saveCurrentMode(mode)
                    // Update local state immediately for UI responsiveness
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onOpacityChanged = { opacity ->
                    // Save opacity and update UI state
                    flashcardUiPreferences.saveOpacity(opacity)
                    // Update local state immediately for real-time preview
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onShowModeSelector = {
                    // Show the mode selection modal
                    flashcardUiPreferences.saveModalVisible(true)
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onHideModeSelector = {
                    // Hide the mode selection modal
                    flashcardUiPreferences.saveModalVisible(false)
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onManageCards = {
                    // Navigate to main app and open categories/flashcard management
                    onManageCards()
                },
                onClose = onClose
            )
        } else {
            // Regular flashcard - use standard container
            FlashcardContainer(
                flashcard = flashcard,
                category = category,
                uiState = currentUiState,
                theme = currentFlashcardTheme,
                onPositionChange = onPositionChange,
                onSizeChange = onSizeChange,
                onModeSelected = { mode ->
                    // Enhanced mode selection - cleaner than old cycling logic
                    flashcardUiPreferences.saveCurrentMode(mode)
                    // Update local state immediately for UI responsiveness
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onOpacityChanged = { opacity ->
                    // Save opacity and update UI state
                    flashcardUiPreferences.saveOpacity(opacity)
                    // Update local state immediately for real-time preview
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onShowModeSelector = {
                    // Show the mode selection modal
                    flashcardUiPreferences.saveModalVisible(true)
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onHideModeSelector = {
                    // Hide the mode selection modal
                    flashcardUiPreferences.saveModalVisible(false)
                    currentUiState = flashcardUiPreferences.getFlashcardUiState()
                },
                onRating = onRating,
                onClose = onClose
            )
        }
    }
    
    /**
     * Handles mode reset after user interaction.
     * Follows DRY principle by centralizing mode reset logic.
     */
    fun resetToNormalMode() {
        flashcardUiPreferences.saveCurrentMode(InteractionMode.NORMAL)
        Log.d(TAG, "Auto-reset interaction mode to NORMAL after rating")
    }
}
