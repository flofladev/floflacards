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

import androidx.compose.foundation.BorderStroke
// Removed isSystemInDarkTheme import - using MaterialTheme.colorScheme instead
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

/**
 * Reusable status badge component following Material Design 3 principles
 * Used in both flashcard and category cards
 */
@Composable
fun StatusBadge(
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                //Lighter gray that matches the theme better
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEnabled) 
                    Icons.Filled.CheckCircle 
                else 
                    Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isEnabled) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Reusable square icon button with consistent styling
 * Follows Material Design 3 guidelines
 */
@Composable
fun ModernSquareIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    isEnabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) containerColor else disabledContainerColor,
            contentColor = if (isEnabled) contentColor else disabledContentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Reusable content card with conditional styling based on enabled state
 * Reduces duplication between question/answer cards and category name cards
 */
@Composable
fun ContentCard(
    isEnabled: Boolean,
    primaryContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                primaryContainerColor.copy(alpha = 0.3f)
            } else {
                //Maintain color identity but very transparent for disabled state
                primaryContainerColor.copy(alpha = 0.08f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        content = { Column(modifier = Modifier.padding(12.dp), content = content) }
    )
}

/**
 * Helper function to detect if we're in light mode
 * Uses MaterialTheme.colorScheme background luminance to determine theme
 */
@Composable
fun isLightMode(): Boolean {
    val backgroundColor = MaterialTheme.colorScheme.background
    // Calculate luminance: if > 0.5, it's light mode
    val red = backgroundColor.red
    val green = backgroundColor.green
    val blue = backgroundColor.blue
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return luminance > 0.5f
}

/**
 * Card border calculation helper
 * Centralizes border logic for consistent styling
 */
@Composable
fun getCardBorder(isEnabled: Boolean): BorderStroke {
    return if (isEnabled) 
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) 
    else 
        // More visible grayish border for disabled state
        BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
}

/**
 * Light mode only card border - returns border only in light mode
 * Used for home screen elements that should only have borders in light mode
 */
@Composable
fun getLightModeOnlyBorder(isEnabled: Boolean): BorderStroke? {
    return if (isLightMode() && isEnabled) 
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) 
    else 
        null
}

/**
 * Container color calculation helper
 * Centralizes container color logic
 */
@Composable
fun getCardContainerColor(isEnabled: Boolean): Color {
    return if (isEnabled) {
        MaterialTheme.colorScheme.surface
    } else {
        //Lighter surface for disabled state that matches theme better
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
}

/**
 * Content alpha calculation helper
 * Centralizes alpha logic for disabled states
 */
fun getContentAlpha(isEnabled: Boolean): Float {
    return if (isEnabled) 1f else 0.45f  // Lighter for better disabled appearance
}

/**
 * Darker primary color helper
 * Creates a much darker version of the primary color for home screen elements
 * Centralizes the dark purple color logic to avoid code duplication
 */
@Composable
fun getDarkerPrimaryColor(): Color {
    return MaterialTheme.colorScheme.primary.copy(
        red = MaterialTheme.colorScheme.primary.red * 0.6f,
        green = MaterialTheme.colorScheme.primary.green * 0.6f,
        blue = MaterialTheme.colorScheme.primary.blue * 0.6f,
        alpha = 1f
    )
}

/**
 * Theme-aware header container color
 * Provides proper contrast for headers in both light and dark modes
 */
@Composable
fun getHeaderContainerColor(): Color {
    // Use theme-aware primary container color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.primaryContainer
}

/**
 * Theme-aware header content color
 * Provides proper text color for headers with good contrast
 */
@Composable
fun getHeaderContentColor(): Color {
    // Use theme-aware content color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onPrimaryContainer
}

/**
 * Theme-aware active status card color
 * Matches header color in light theme for consistency
 */
@Composable
fun getActiveStatusCardColor(): Color {
    // Use theme-aware primary container color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.primaryContainer
}

/**
 * Theme-aware active status card content color
 * Matches header content color for consistency
 */
@Composable
fun getActiveStatusCardContentColor(): Color {
    // Use theme-aware content color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onPrimaryContainer
}

/**
 * Theme-aware settings card color
 * Uses subtle tertiary colors similar to the contact developer button
 */
@Composable
fun getSettingsCardColor(): Color {
    // Use the same color scheme as the contact developer button for consistency
    return MaterialTheme.colorScheme.tertiaryContainer
}

/**
 * Theme-aware donation icon color
 * Provides better visibility for donation dialog icons in light mode
 */
@Composable
fun getDonationIconColor(): Color {
    // Use theme-aware surface variant color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Theme-aware donation tab selected background color
 * Provides better contrast for selected donation tabs
 */
@Composable
fun getDonationTabSelectedColor(): Color {
    // Use theme-aware primary container color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.primaryContainer
}

/**
 * Theme-aware donation tab selected text/icon color
 * Provides proper contrast for text on selected donation tabs
 */
@Composable
fun getDonationTabSelectedContentColor(): Color {
    // Use theme-aware content color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onPrimaryContainer
}
