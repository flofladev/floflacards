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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.data.model.FlashcardTheme

/**
 * Helper function to get localized display name for InteractionMode in FlashcardHeader
 */
@Composable
fun InteractionMode.getLocalizedDisplayName(): String {
    return when (this) {
        InteractionMode.NORMAL -> "" // Not used for NORMAL mode
        InteractionMode.DRAG -> stringResource(R.string.interaction_mode_drag)
        InteractionMode.RESIZE -> stringResource(R.string.interaction_mode_resize)
        InteractionMode.OPACITY -> stringResource(R.string.interaction_mode_opacity)
    }
}

/**
 * Enhanced Header component with space-optimized mode indication
 * Updated to follow SOLID principles and eliminate cycling logic (DRY)
 * NEW DESIGN: Dynamic category names replace separate mode indicators
 * 
 * This component provides:
 * - Dynamic category display (shows mode name when active, category when normal)
 * - Settings button that opens mode selection modal
 * - Close button
 * - Drag gesture detection when in drag mode
 * - Color-coded header background for visual mode indication
 * - Space-efficient design without redundant UI elements
 * 
 * Why this design is superior:
 * - KISS: Single text element instead of category + mode badge
 * - DRY: Eliminates duplicate visual indicators, uses centralized colors
 * - SOLID: Single responsibility for header display
 * - Space-efficient: Saves horizontal space by removing redundant mode badges
 */
@Composable
fun FlashcardHeader(
    category: CategoryEntity?,
    currentMode: InteractionMode,
    theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
    onPositionChange: (Int, Int) -> Unit,
    onShowModeSelector: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                FlashcardColors.getHeaderBackgroundColor(theme, currentMode.name),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .then(
                if (currentMode == InteractionMode.DRAG) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            // Apply drag amount directly as relative offset
                            // This will move the window by the exact amount dragged
                            onPositionChange(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    }
                } else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enhanced Settings button - opens mode selector modal
        IconButton(
            onClick = onShowModeSelector,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Select Interaction Mode",
                tint = FlashcardColors.getTextColor(theme)
            )
        }
        
        // Dynamic category name that shows mode when active
        // Following KISS principle - single text element saves space
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "📁",
                fontSize = 14.sp
            )
            
            Text(
                text = currentMode.getCategoryDisplayName(category?.name) ?: currentMode.getLocalizedDisplayName(),
                color = FlashcardColors.getTextColor(theme),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            )
        }
        
        // Close button - compact size
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = FlashcardColors.getTextColor(theme)
            )
        }
    }
}
