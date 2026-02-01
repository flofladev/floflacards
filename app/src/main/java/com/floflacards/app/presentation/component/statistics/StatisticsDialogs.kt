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

package com.floflacards.app.presentation.component.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floflacards.app.presentation.viewmodel.CategoryStats
import com.floflacards.app.presentation.viewmodel.FlashcardStats
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

@Composable
fun ResetAllStatisticsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    currentStreak: Int,
    highestStreak: Int,
    totalFlashcards: Int
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getStatisticsSurface(),
        title = {
            Text(
                text = stringResource(R.string.reset_all_stats_title),
                fontWeight = FontWeight.SemiBold,
                color = getStatisticsOnSurface()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reset_all_stats_description),
                    color = getStatisticsOnSurfaceVariant(),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.reset_all_stats_flashcards, totalFlashcards),
                    color = getStatisticsOnSurface(),
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.reset_all_stats_current_streak, currentStreak),
                    color = getStatisticsOnSurface(),
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.reset_all_stats_highest_streak, highestStreak),
                    color = getStatisticsOnSurface(),
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.reset_all_stats_history),
                    color = getStatisticsOnSurface(),
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.reset_all_stats_warning),
                    color = AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.reset_all_stats_confirm),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = getStatisticsOnSurface()
                )
            ) {
                Text(stringResource(R.string.reset_all_stats_cancel))
            }
        }
    )
}

@Composable
fun FlashcardResetConfirmationDialog(
    flashcard: FlashcardStats,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getStatisticsSurface(),
        title = {
            Text(
                text = stringResource(R.string.reset_flashcard_stats_title),
                fontWeight = FontWeight.SemiBold,
                color = getStatisticsOnSurface()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reset_flashcard_stats_description),
                    color = getStatisticsOnSurfaceVariant(),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Flashcard preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = getStatisticsSurfaceVariant()),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = flashcard.question,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = getStatisticsOnSurface(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = flashcard.answer,
                            fontSize = 12.sp,
                            color = getStatisticsOnSurfaceVariant(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.reset_flashcard_stats_details),
                    color = getStatisticsOnSurfaceVariant(),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.reset_flashcard_stats_warning),
                    color = AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.reset_flashcard_stats_confirm),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = getStatisticsOnSurface()
                )
            ) {
                Text(stringResource(R.string.reset_flashcard_stats_cancel))
            }
        }
    )
}

@Composable
fun CategoryResetConfirmationDialog(
    category: CategoryStats,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getStatisticsSurface(),
        title = {
            Text(
                text = stringResource(R.string.reset_category_stats_title),
                fontWeight = FontWeight.SemiBold,
                color = getStatisticsOnSurface()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reset_category_stats_description),
                    color = getStatisticsOnSurfaceVariant(),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = getStatisticsSurfaceVariant()),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = category.categoryName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = getStatisticsOnSurface()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.stats_cards_suffix, category.totalCards),
                            fontSize = 12.sp,
                            color = getStatisticsOnSurfaceVariant()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.reset_category_stats_details, category.totalCards),
                    color = getStatisticsOnSurfaceVariant(),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.reset_category_stats_warning),
                    color = AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.reset_category_stats_confirm),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = getStatisticsOnSurface()
                )
            ) {
                Text(stringResource(R.string.reset_category_stats_cancel))
            }
        }
    )
}
