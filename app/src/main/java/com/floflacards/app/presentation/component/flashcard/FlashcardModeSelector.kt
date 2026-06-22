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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.presentation.component.FlashcardColors
import com.floflacards.app.presentation.component.text.AutoSizeText

/**
 * Helper function to get localized display name for InteractionMode
 */
@Composable
fun InteractionMode.getDisplayName(): String {
    return when (this) {
        InteractionMode.NORMAL -> stringResource(R.string.interaction_mode_learning)
        InteractionMode.DRAG -> stringResource(R.string.interaction_mode_drag)
        InteractionMode.RESIZE -> stringResource(R.string.interaction_mode_resize)
        InteractionMode.OPACITY -> stringResource(R.string.interaction_mode_opacity)
    }
}

/**
 * Material icon for each mode (kept in the UI layer so the domain enum stays Compose-free):
 * learning (book), move (open-with arrows), resize (aspect ratio), opacity (droplet).
 */
fun InteractionMode.getModeIcon(): ImageVector = when (this) {
    InteractionMode.NORMAL -> Icons.AutoMirrored.Filled.MenuBook
    InteractionMode.DRAG -> Icons.Filled.OpenWith
    InteractionMode.RESIZE -> Icons.Filled.AspectRatio
    InteractionMode.OPACITY -> Icons.Filled.Opacity
}

/**
 * FlashcardModeSelector - Service-compatible modal for selecting interaction modes
 * Following SOLID principles - Single Responsibility for mode selection
 * 
 * Fixed version that works in Service context without Dialog wrapper
 * 
 * Features:
 * - 4 distinct mode cards (Normal, Drag, Resize, Opacity)
 * - Visual mode indicators with color coding
 * - Smooth animations and transitions
 * - Opacity controls integrated for opacity mode
 * - Service-compatible overlay design
 */
@Composable
fun FlashcardModeSelector(
    isVisible: Boolean,
    currentMode: InteractionMode,
    onModeSelected: (InteractionMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        // Semi-transparent backdrop that covers the entire overlay; tapping it dismisses.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Full-bleed panel: fills the card so the 4 tiles always tile it into equal
            // quadrants regardless of card size/orientation (no fixed sizes to overflow).
            Card(
                modifier = modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clickable(enabled = false) { /* Prevent backdrop dismissal */ },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FlashcardColors.getBackgroundColor(theme)
                ),
                // No elevation: see ModeCard — a shadow in the overlay window renders as a square
                // shade artifact rather than a soft drop shadow.
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Mode selection grid — two equal-height rows, two equal-width tiles each.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Normal and Drag modes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModeCard(
                                mode = InteractionMode.NORMAL,
                                isSelected = currentMode == InteractionMode.NORMAL,
                                onSelected = onModeSelected,
                                theme = theme,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )

                            ModeCard(
                                mode = InteractionMode.DRAG,
                                isSelected = currentMode == InteractionMode.DRAG,
                                onSelected = onModeSelected,
                                theme = theme,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        // Row 2: Resize and Opacity modes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModeCard(
                                mode = InteractionMode.RESIZE,
                                isSelected = currentMode == InteractionMode.RESIZE,
                                onSelected = onModeSelected,
                                theme = theme,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )

                            ModeCard(
                                mode = InteractionMode.OPACITY,
                                isSelected = currentMode == InteractionMode.OPACITY,
                                onSelected = onModeSelected,
                                theme = theme,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }

                    // Compact close affordance overlaid in the corner (tiles auto-close on
                    // selection, and the backdrop is tappable, so this stays small).
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FlashcardColors.getTextColor(theme),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual mode card component
 */
@Composable
private fun ModeCard(
    mode: InteractionMode,
    isSelected: Boolean,
    onSelected: (InteractionMode) -> Unit,
    theme: FlashcardTheme,
    modifier: Modifier = Modifier
) {
    // NORMAL's primary color is white, which is invisible on a light panel. Fall back to the
    // theme's text color for it; the other modes' saturated accents read fine on both themes.
    val accentColor = if (mode == InteractionMode.NORMAL) {
        FlashcardColors.getTextColor(theme)
    } else {
        Color(mode.getPrimaryColor())
    }
    val baseTextColor = FlashcardColors.getTextColor(theme)

    Card(
        modifier = modifier
            .clickable { onSelected(mode) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.2f)
            } else {
                FlashcardColors.getHeaderBackgroundColor(theme)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else null,
        // No elevation: in the overlay window a Material shadow has nowhere to fall and instead
        // fills the rounded corners as a square shade artifact (same reason the main card uses 0).
        // Selection is conveyed by the border + tinted background instead.
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mode icon
            Icon(
                imageVector = mode.getModeIcon(),
                contentDescription = null,
                tint = if (isSelected) accentColor else baseTextColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mode name with auto text sizing for visibility
            AutoSizeText(
                text = mode.getDisplayName(),
                color = if (isSelected) accentColor else baseTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                minTextSize = 8.sp,
                maxTextSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            // Selected indicator
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
