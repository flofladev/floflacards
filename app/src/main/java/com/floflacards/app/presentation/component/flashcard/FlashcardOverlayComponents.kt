package com.floflacards.app.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.domain.model.FlashcardRating
import com.floflacards.app.data.source.FlashcardUiPreferences
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.data.model.FlashcardTheme

/**
 * Enhanced flashcard container with modal-based interaction system
 * Updated to follow SOLID principles and support all interaction modes
 * NEW DESIGN: Dark background theme with centralized colors
 * 
 * Key improvements:
 * - KISS: Single modal for mode selection
 * - DRY: Eliminates duplicate mode switching logic, uses centralized colors
 * - Support for opacity mode with real-time preview
 * - Better visual feedback for all modes
 * - Dark background theme consistent with new design
 */
@Composable
fun FlashcardContainer(
    flashcard: FlashcardEntity,
    category: CategoryEntity?,
    uiState: FlashcardUiPreferences.FlashcardUiState,
    theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
    onPositionChange: (Int, Int) -> Unit,
    onSizeChange: (Int, Int) -> Unit,
    onModeSelected: (InteractionMode) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onShowModeSelector: () -> Unit,
    onHideModeSelector: () -> Unit,
    onRating: (FlashcardRating) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAnswer by remember { mutableStateOf(false) }
    
    // For overlay windows, positioning is handled by WindowManager.LayoutParams
    // Compose content should fill the entire overlay window
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                // Enhanced visual indicators for all modes
                when (uiState.currentMode) {
                    InteractionMode.DRAG -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF4CAF50).copy(alpha = 0.9f), // Green
                        shape = RoundedCornerShape(20.dp)
                    )
                    InteractionMode.RESIZE -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFFFF9800).copy(alpha = 0.9f), // Orange
                        shape = RoundedCornerShape(20.dp)
                    )
                    InteractionMode.OPACITY -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF2196F3).copy(alpha = 0.9f), // Blue
                        shape = RoundedCornerShape(20.dp)
                    )
                    InteractionMode.NORMAL -> Modifier
                }
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .alpha(uiState.getAlpha()),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = FlashcardColors.getBackgroundColor(theme)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Enhanced Header with modal support
                FlashcardHeader(
                    category = category,
                    currentMode = uiState.currentMode,
                    theme = theme,
                    onPositionChange = onPositionChange,
                    onShowModeSelector = onShowModeSelector,
                    onClose = onClose
                )
                
                // Content area
                FlashcardContent(
                    flashcard = flashcard,
                    showAnswer = showAnswer,
                    onShowAnswer = { showAnswer = true },
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                
                // Controls (rating buttons) - only show when answer is visible
                if (showAnswer) {
                    FlashcardControls(
                        onRating = onRating,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                
                // Compact Opacity Slider - show when in opacity mode
                CompactOpacitySlider(
                    isVisible = uiState.currentMode == InteractionMode.OPACITY,
                    currentOpacity = uiState.opacity,
                    onOpacityChange = onOpacityChanged
                )
            }
        }
        
        // Resize handles - only show in resize mode
        if (uiState.currentMode == InteractionMode.RESIZE) {
            ResizeHandles(
                onSizeChange = onSizeChange,
                currentWidth = uiState.width,
                currentHeight = uiState.height
            )
        }
        
        // Mode selection modal - simplified without opacity controls
        FlashcardModeSelector(
            isVisible = uiState.isModalVisible,
            currentMode = uiState.currentMode,
            onModeSelected = { mode ->
                onModeSelected(mode)
                onHideModeSelector()
            },
            onDismiss = onHideModeSelector
        )
    }
}

