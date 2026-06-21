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

package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.presentation.theme.onStopColor
import com.floflacards.app.presentation.theme.onSuccessColor
import com.floflacards.app.presentation.theme.onWarningColor
import com.floflacards.app.presentation.theme.onWarningContainerColor
import com.floflacards.app.presentation.theme.stopColor
import com.floflacards.app.presentation.theme.successColor
import com.floflacards.app.presentation.theme.warningColor
import com.floflacards.app.presentation.theme.warningContainerColor

/**
 * Learning controls component following SRP.
 * Handles only learning-related UI and state.
 * Follows KISS principle with simple, focused functionality.
 * UPDATED: Unified button approach - replaces old code with single button responsibility
 */
@Composable
fun LearningControls(
    isServiceActive: Boolean,
    nextFlashcardCountdown: Long,
    activeFlashcardCount: Int,
    hasOverlayPermission: Boolean,
    onStartLearning: () -> Unit,
    onStopLearning: () -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateToCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Unified Learning/Navigation Button (replaces old button + hint card)
        UnifiedLearningButton(
            isServiceActive = isServiceActive,
            hasOverlayPermission = hasOverlayPermission,
            activeFlashcardCount = activeFlashcardCount,
            nextFlashcardCountdown = nextFlashcardCountdown,
            onStartLearning = onStartLearning,
            onStopLearning = onStopLearning,
            onRequestPermission = onRequestPermission,
            onNavigateToCards = onNavigateToCards
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Only show permission warning for users who have cards but no overlay permission
        if (activeFlashcardCount > 0 && !hasOverlayPermission && !isServiceActive) {
            PermissionWarningCard()
        }
    }
}

/**
 * Visual + behavioural description of the unified button for a given app state.
 * Keeping it in one place means the four states stay mutually exclusive and easy to reason about.
 */
private data class LearningButtonState(
    val text: String,
    val container: Color,
    val content: Color,
    val action: () -> Unit,
    val compactText: Boolean = false
)

/**
 * UNIFIED Learning/Navigation button - follows SOLID, DRY, KISS principles.
 * Single responsibility: handles learning actions AND navigation hints.
 * Replaces old LearningButton + NoFlashcardsHintCard to eliminate duplication.
 *
 * Colours come from the theme (success/warning accessors + colorScheme) so the button is correct
 * in both light and dark. When a session is active the button also carries the live countdown,
 * which is why the separate status/countdown card is no longer needed.
 */
@Composable
private fun UnifiedLearningButton(
    isServiceActive: Boolean,
    hasOverlayPermission: Boolean,
    activeFlashcardCount: Int,
    nextFlashcardCountdown: Long,
    onStartLearning: () -> Unit,
    onStopLearning: () -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateToCards: () -> Unit
) {
    val state = when {
        // Service is active - stop learning, and surface the live countdown inline
        isServiceActive -> LearningButtonState(
            text = stringResource(R.string.learning_stop_with_countdown, nextFlashcardCountdown),
            container = stopColor(),
            content = onStopColor(),
            action = onStopLearning
        )
        // No flashcards - navigate to cards (replaces NoFlashcardsHintCard)
        activeFlashcardCount == 0 -> LearningButtonState(
            text = stringResource(R.string.learning_no_cards_hint),
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
            action = onNavigateToCards,
            compactText = true
        )
        // Has flashcards but no overlay permission - request permission
        !hasOverlayPermission -> LearningButtonState(
            text = stringResource(R.string.learning_permission_required_button),
            container = warningColor(),
            content = onWarningColor(),
            action = onRequestPermission
        )
        // Ready to start learning
        else -> LearningButtonState(
            text = stringResource(R.string.learning_start_button_caps),
            container = successColor(),
            content = onSuccessColor(),
            action = onStartLearning
        )
    }

    Button(
        onClick = state.action,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = state.container,
            contentColor = state.content
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = state.text,
            color = state.content,
            fontSize = if (state.compactText) 14.sp else 16.sp, // Smaller font for longer hint text
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Old NoFlashcardsHintCard and LearningWarnings removed - functionality moved to UnifiedLearningButton
// This eliminates code duplication and follows DRY principle

/**
 * Warning card for overlay permission requirement.
 * Maintains consistent styling with the no flashcards card.
 */
@Composable
private fun PermissionWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = warningContainerColor()
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = stringResource(R.string.learning_overlay_permission_warning),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = onWarningContainerColor(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
