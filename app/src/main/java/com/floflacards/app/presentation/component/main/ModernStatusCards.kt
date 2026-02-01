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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.presentation.component.text.AutoSizeText


@Composable
fun ModernLearningStatusGrid(
    isServiceActive: Boolean,
    activeFlashcardCount: Int,
    streak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.status_learning_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatusCard(
                    icon = "🔄",
                    label = stringResource(R.string.status_service_label),
                    value = if (isServiceActive) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
                    isPositive = isServiceActive,
                    useAutoSizeText = true, // All status cards now use consistent auto-sizing
                    modifier = Modifier.weight(1f)
                )
                
                ModernStatusCard(
                    icon = "📚",
                    label = stringResource(R.string.status_active_cards),
                    value = stringResource(R.string.stats_cards_suffix, activeFlashcardCount),
                    isPositive = activeFlashcardCount > 0,
                    useAutoSizeText = true,
                    modifier = Modifier.weight(1f)
                )
                
                ModernStatusCard(
                    icon = "🔥",
                    label = stringResource(R.string.status_streak_days),
                    value = "$streak ${stringResource(R.string.status_days_text)}",
                    isPositive = streak > 0,
                    useAutoSizeText = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernStatusCard(
    icon: String,
    label: String,
    value: String,
    isPositive: Boolean,
    useAutoSizeText: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isPositive -> getActiveStatusCardColor()
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when {
        isPositive -> getActiveStatusCardContentColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            if (useAutoSizeText) {
                AutoSizeText(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    minTextSize = 10.sp,
                    modifier = Modifier
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
