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

package com.floflacards.app.service

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.domain.model.FlashcardRating
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.data.source.FlashcardUiPreferences
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.data.model.GlowIntensity
import com.floflacards.app.presentation.component.FlashcardColors
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

        // Card fade-in after the glow hands over; makeWindowTouchable() waits
        // this long so touch only arrives with a fully visible card.
        private const val CARD_FADE_IN_MS = 300

        // Glow timeline building blocks; the user's duration setting decides
        // how many breaths fit between the fades.
        private const val GLOW_FADE_IN_MS = 450
        private const val GLOW_FADE_OUT_MS = 250
        private const val GLOW_BREATH_MS = 800
    }

    /**
     * Entrance cue played before a card turns interactive: a soft glow bleeds in
     * from the screen edge nearest the card's spot (Samsung-edge-panel style),
     * takes one slow breath and fades — "a card lands here in a second" — then
     * the card fades in at its real position. The point is that a card landing
     * in the user's thumb zone announces itself instead of materializing under a
     * finger mid-tap.
     *
     * The window IS the glow strip during the cue (see OverlayManager) and is
     * FLAG_NOT_TOUCHABLE, so taps there still reach the app underneath.
     * [onGlowDone] fires when the glow has faded and must snap the window to the
     * card's geometry; [onRevealed] fires once the card is fully visible and
     * must flip the window touchable again. With [enabled] false the content
     * renders immediately and neither callback fires.
     *
     * The timeline runs only while [cardVisible] holds: a card arriving hidden
     * (keyboard open, landscape) plays its glow when the arbiter reveals it, not
     * invisibly in the background. Hidden again mid-glow → the glow replays from
     * the top on the next reveal; hidden after the handover → only the remaining
     * touch-enable step is retried.
     *
     * [durationSeconds] is the total glow time before the handover (the breath
     * loops to fill it); [intensity] scales the gradient alphas — its strip
     * width half lives in OverlayManager's window geometry.
     */
    @Composable
    fun EntranceCue(
        enabled: Boolean,
        cardVisible: State<Boolean>,
        fromLeftEdge: State<Boolean>,
        durationSeconds: Int,
        intensity: GlowIntensity,
        onGlowDone: () -> Unit,
        onRevealed: () -> Unit,
        content: @Composable () -> Unit
    ) {
        if (!enabled) {
            content()
            return
        }

        val theme by settingsManager.flashcardTheme.collectAsState()
        val visible by cardVisible
        val glowAlpha = remember { Animatable(0f) }
        var showCard by remember { mutableStateOf(false) }
        var revealed by remember { mutableStateOf(false) }

        LaunchedEffect(visible) {
            if (!visible || revealed) return@LaunchedEffect
            if (!showCard) {
                // Fill the user's chosen duration: fade in, breathe for as long
                // as the budget allows, hold at peak for any remainder shorter
                // than a full breath (a truncated half-breath reads as a glitch),
                // then fade out into the handover.
                glowAlpha.snapTo(0f)
                glowAlpha.animateTo(1f, tween(GLOW_FADE_IN_MS, easing = FastOutSlowInEasing))
                var breatheBudgetMs =
                    durationSeconds * 1000L - GLOW_FADE_IN_MS - GLOW_FADE_OUT_MS
                while (breatheBudgetMs >= GLOW_BREATH_MS) {
                    glowAlpha.animateTo(0.35f, tween(GLOW_BREATH_MS / 2, easing = FastOutSlowInEasing))
                    glowAlpha.animateTo(1f, tween(GLOW_BREATH_MS / 2, easing = FastOutSlowInEasing))
                    breatheBudgetMs -= GLOW_BREATH_MS
                }
                if (breatheBudgetMs > 0) delay(breatheBudgetMs)
                glowAlpha.animateTo(0f, tween(GLOW_FADE_OUT_MS))
                showCard = true
                onGlowDone()
            }
            delay(CARD_FADE_IN_MS.toLong())
            onRevealed()
            revealed = true
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!showCard) {
                // Shadow-like gradient anchored to the screen edge: a bright-ish
                // seam at the very edge dissolving inward, in the card theme's accent.
                val accent = FlashcardColors.getAccentColor(theme)
                val glowColors = listOf(
                    accent.copy(alpha = intensity.peakAlpha),
                    accent.copy(alpha = intensity.midAlpha),
                    Color.Transparent
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = glowAlpha.value }
                        .background(
                            Brush.horizontalGradient(
                                if (fromLeftEdge.value) glowColors else glowColors.reversed()
                            )
                        )
                )
            }
            AnimatedVisibility(
                visible = showCard,
                enter = fadeIn(animationSpec = tween(CARD_FADE_IN_MS))
            ) {
                content()
            }
        }
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
        onSnooze: (Int) -> Unit = { }, // Minutes; only reachable from regular flashcards
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
                onClose = onClose,
                onSnooze = onSnooze
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
