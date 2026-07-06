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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floflacards.app.R
import com.floflacards.app.data.model.FlashcardTheme

/**
 * "Not now?" chooser opened by the header X on a regular flashcard. The tap on X
 * is exactly the moment the card is unwelcome, so this is where the user gets to
 * say for how long: skip just this card, or snooze the whole session.
 *
 * Same service-compatible construction as FlashcardModeSelector: tappable
 * backdrop, full-bleed zero-elevation panel (a Material shadow renders as a
 * square shade artifact inside the overlay window).
 */
@Composable
fun FlashcardCloseMenu(
    isVisible: Boolean,
    onSkip: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clickable(enabled = false) { /* Prevent backdrop dismissal */ },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FlashcardColors.getBackgroundColor(theme)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.flashcard_close_menu_title),
                        color = FlashcardColors.getTextColor(theme),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Skip first and biggest: reflex-closers land on it with two fast
                    // taps in the same thumb zone the X occupies.
                    CloseMenuRow(
                        icon = Icons.Filled.SkipNext,
                        label = stringResource(R.string.flashcard_close_skip),
                        accented = true,
                        theme = theme,
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    )
                    CloseMenuRow(
                        icon = Icons.Filled.Snooze,
                        label = stringResource(R.string.flashcard_close_snooze_30m),
                        accented = false,
                        theme = theme,
                        onClick = { onSnooze(30) },
                        modifier = Modifier.weight(1f)
                    )
                    CloseMenuRow(
                        icon = Icons.Filled.Snooze,
                        label = stringResource(R.string.flashcard_close_snooze_2h),
                        accented = false,
                        theme = theme,
                        onClick = { onSnooze(120) },
                        modifier = Modifier.weight(1f)
                    )
                    CloseMenuRow(
                        icon = Icons.Filled.Snooze,
                        label = stringResource(R.string.flashcard_close_snooze_1d),
                        accented = false,
                        theme = theme,
                        onClick = { onSnooze(1440) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CloseMenuRow(
    icon: ImageVector,
    label: String,
    accented: Boolean,
    theme: FlashcardTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = FlashcardColors.getTextColor(theme)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accented) {
                textColor.copy(alpha = 0.2f)
            } else {
                FlashcardColors.getHeaderBackgroundColor(theme)
            }
        ),
        border = if (accented) BorderStroke(2.dp, textColor) else null,
        // No elevation: same overlay-window shadow artifact as the mode selector tiles.
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (accented) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
