package com.floflacards.app.presentation.component.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.source.FlashcardUiPreferences
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.presentation.component.FlashcardColors
import com.floflacards.app.presentation.component.CompactOpacitySlider
import com.floflacards.app.presentation.component.ResizeHandles
import com.floflacards.app.presentation.component.FlashcardHeader
import com.floflacards.app.presentation.component.FlashcardModeSelector

/**
 * Specialized container for empty state flashcards.
 * Reuses existing FlashcardContainer structure but customizes UI for empty state.
 * Follows DRY principle by leveraging existing components and theming.
 */
@Composable
fun EmptyStateFlashcardContainer(
    flashcard: FlashcardEntity,
    uiState: FlashcardUiPreferences.FlashcardUiState,
    theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
    onPositionChange: (Int, Int) -> Unit,
    onSizeChange: (Int, Int) -> Unit,
    onModeSelected: (InteractionMode) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onShowModeSelector: () -> Unit,
    onHideModeSelector: () -> Unit,
    onManageCards: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Validation: Ensure this is actually an empty state flashcard
    require(flashcard.id == -2L) { "EmptyStateFlashcardContainer should only be used with empty state flashcards (ID -2L)" }
    
    // Minimalistic empty state - no show/hide answer logic needed
    
    // For overlay windows, positioning is handled by WindowManager.LayoutParams
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                // Visual indicators for interaction modes
                when (uiState.currentMode) {
                    InteractionMode.DRAG -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    InteractionMode.RESIZE -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFFFF9800).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    InteractionMode.OPACITY -> Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF2196F3).copy(alpha = 0.9f),
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
                // Header with gear, info, and close buttons
                FlashcardHeader(
                    category = null, // No category for empty state
                    currentMode = uiState.currentMode,
                    theme = theme,
                    onPositionChange = onPositionChange,
                    onShowModeSelector = onShowModeSelector,
                    onClose = onClose
                )
                
                // Minimalistic content area with just message and manage button
                MinimalisticEmptyContent(
                    theme = theme,
                    onManageCards = onManageCards,
                    modifier = Modifier.weight(1f)
                )
                
                // Opacity slider - only show when in opacity mode
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
        
        // Mode selector modal
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

/**
 * Minimalistic empty state content with just a message and manage button.
 * Replaces the complex flashcard content structure for a cleaner UX.
 */
@Composable
private fun MinimalisticEmptyContent(
    theme: FlashcardTheme,
    onManageCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simple message
        Text(
            text = stringResource(R.string.empty_state_no_cards_title),
            color = FlashcardColors.getTextColor(theme),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Manage button (replaces "show answer" functionality)
        Button(
            onClick = onManageCards,
            colors = FlashcardColors.getShowAnswerButtonColors(theme),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(44.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_manage_flashcards),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
