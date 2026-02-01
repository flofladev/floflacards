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
 * Status card component following SRP.
 */
@Composable
private fun StatusCard(
    isServiceActive: Boolean,
    nextFlashcardCountdown: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceActive) Color(0xFF4CAF50) else Color(0xFF757575)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Status: ${if (isServiceActive) "Active" else "Inactive"}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (isServiceActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next flashcard in: ${formatTime(nextFlashcardCountdown)}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * UNIFIED Learning/Navigation button - follows SOLID, DRY, KISS principles.
 * Single responsibility: handles learning actions AND navigation hints.
 * Replaces old LearningButton + NoFlashcardsHintCard to eliminate duplication.
 */
@Composable
private fun UnifiedLearningButton(
    isServiceActive: Boolean,
    hasOverlayPermission: Boolean,
    activeFlashcardCount: Int,
    onStartLearning: () -> Unit,
    onStopLearning: () -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateToCards: () -> Unit
) {
    val (buttonText, buttonColor, buttonAction) = when {
        // Service is active - stop learning
        isServiceActive -> Triple(
            stringResource(R.string.learning_stop_button_caps),
            Color(0xFFE91E63), // Pink/red for stop
            onStopLearning
        )
        // No flashcards - navigate to cards (replaces NoFlashcardsHintCard)
        activeFlashcardCount == 0 -> Triple(
            stringResource(R.string.learning_no_cards_hint),
            Color(0xFF1DB096), // Purple theme to match app design
            onNavigateToCards
        )
        // Has flashcards but no overlay permission - request permission
        !hasOverlayPermission -> Triple(
            stringResource(R.string.learning_permission_required_button),
            Color(0xFFFF9800), // Orange for permission
            onRequestPermission
        )
        // Ready to start learning
        else -> Triple(
            stringResource(R.string.learning_start_button_caps),
            Color(0xFF4CAF50), // Green for go
            onStartLearning
        )
    }
    
    Button(
        onClick = buttonAction,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = true // Always enabled - button handles all states
    ) {
        Text(
            text = buttonText,
            color = Color.White,
            fontSize = if (activeFlashcardCount == 0) 14.sp else 16.sp, // Smaller font for longer hint text
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
            containerColor = Color(0xFFFFF8E1) // Light amber background
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
                color = Color(0xFFFF8F00), // Amber
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Utility function for formatting time.
 * Moved here to follow DRY principle.
 */
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
