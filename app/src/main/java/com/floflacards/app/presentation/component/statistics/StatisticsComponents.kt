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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.presentation.component.text.AutoSizeText
import com.floflacards.app.presentation.viewmodel.CategoryStats
import com.floflacards.app.presentation.viewmodel.FlashcardStats
import com.floflacards.app.presentation.viewmodel.EnhancedOverallStats

@Composable
fun ModernStatsCardGrid(stats: EnhancedOverallStats) {
    // Slim two-card summary: streak (current + best folded into one) and mastery. The previous
    // three-tile row duplicated the streak metric; merging removes the redundancy.
    // Both tiles carry a second line (best streak / mastery %) so they share line count and end up
    // the same height — height is content-driven (see StatCard), never fixed, so text can't clip.
    val masteredPercent = if (stats.totalFlashcards > 0) {
        stats.masteredFlashcards * 100 / stats.totalFlashcards
    } else {
        0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            value = "${stats.streakDays}",
            label = stringResource(R.string.stats_current_streak),
            sublabel = stringResource(R.string.stats_best_short, stats.highestStreak),
            accentColor = getStreakAccentColor(),
            backgroundColor = getStreakAccentBackground(),
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = getStreakAccentColor(),
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "${stats.masteredFlashcards}/${stats.totalFlashcards}",
            label = stringResource(R.string.stats_total_flashcards),
            sublabel = stringResource(R.string.stats_percent_short, masteredPercent),
            accentColor = getMasteryAccentColor(),
            backgroundColor = getMasteryAccentBackground(),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = getMasteryAccentColor(),
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}


/**
 * Compact, non-expanding category row. Shows name + mastery progress + average, and navigates to a
 * dedicated detail screen on tap (replacing the old in-place accordion, which nested a scrollable
 * list inside the screen's scroll). The reset action stays inline for quick access.
 */
@Composable
fun CategoryStatRow(
    categoryStats: CategoryStats,
    onClick: () -> Unit,
    onCategoryResetClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getStatisticsSurface()),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = categoryStats.categoryName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = getStatisticsOnSurface(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress indicator for mastered flashcards
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getStatisticsProgressBackground())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(categoryStats.masteredRate)
                                .background(getStatisticsProgressFill())
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.stats_mastered_count, categoryStats.masteredCards, categoryStats.totalCards),
                        fontSize = 14.sp,
                        color = getStatisticsOnSurfaceVariant(),
                        fontWeight = FontWeight.Medium
                    )

                    if (categoryStats.averageSuccessRate > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.stats_average_short, (categoryStats.averageSuccessRate * 100).toInt()),
                            fontSize = 12.sp,
                            color = AccentTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category reset button - only show if category has flashcards with statistics
                if (categoryStats.flashcards.any { it.totalAttempts > 0 }) {
                    IconButton(
                        onClick = { onCategoryResetClick() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.stats_reset_category_description),
                            tint = getStatisticsOnSurfaceVariant(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = getStatisticsOnSurfaceVariant()
                )
            }
        }
    }
}

/**
 * Legend explaining the per-flashcard red/amber/green attempt chips and the average symbol, shown
 * on the category detail screen where those chips appear.
 */
@Composable
fun StatLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = AccentRed, label = stringResource(R.string.stats_legend_incorrect))
        LegendDot(color = AccentAmber, label = stringResource(R.string.stats_legend_hard))
        LegendDot(color = AccentGreen, label = stringResource(R.string.stats_legend_correct))
        Text(
            text = stringResource(R.string.stats_legend_average),
            fontSize = 11.sp,
            color = getStatisticsOnSurfaceVariant()
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = getStatisticsOnSurfaceVariant()
        )
    }
}

@Composable
fun FlashcardStatItem(
    flashcard: FlashcardStats,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getStatisticsSurfaceVariant()),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row with question and reset button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flashcard.question,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getStatisticsOnSurface(),
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = flashcard.answer,
                        fontSize = 13.sp,
                        color = getStatisticsOnSurfaceVariant(),
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Only show reset button if the flashcard has statistics to reset
                if (flashcard.totalAttempts > 0) {
                    IconButton(
                        onClick = onResetClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.stats_reset_flashcard_description),
                            tint = getStatisticsOnSurfaceVariant(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Compact stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactStatChip(
                        count = flashcard.incorrectCount,
                        backgroundColor = AccentRed,
                        textColor = Color.White
                    )
                    CompactStatChip(
                        count = flashcard.hardCount,
                        backgroundColor = AccentAmber,
                        textColor = Color.Black
                    )
                    CompactStatChip(
                        count = flashcard.correctCount,
                        backgroundColor = AccentGreen,
                        textColor = Color.White
                    )
                }
                
                // Success rate and mastery
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${flashcard.successRate.toInt()}%",
                        fontSize = 12.sp,
                        color = AccentTeal,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (flashcard.isMastered) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(AccentGreen, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Last seen in smaller text
            Text(
                text = flashcard.lastSeenText,
                fontSize = 11.sp,
                color = getStatisticsOnSurfaceVariant()
            )
        }
    }
}

@Composable
private fun CompactStatChip(
    count: Int,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = count.toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// Modern Card Components

@Composable
private fun StatCard(
    value: String,
    label: String,
    accentColor: Color,
    backgroundColor: Color,
    sublabel: String? = null,
    icon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = getStatisticsCardBackground()),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Content-driven height with a floor: grows to fit (incl. large system font scales)
                // instead of a fixed height that clips the lower text. Paired tiles share the same
                // line structure, so they end up the same height without intrinsic measurement.
                .heightIn(min = 96.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = getStatisticsCardBorder(),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon at the top
                icon?.invoke()

                Spacer(modifier = Modifier.height(4.dp))

                // Main value with stable AutoSizeText like home screen learning status
                AutoSizeText(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    minTextSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Label with stable AutoSizeText to always show full text
                AutoSizeText(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = getStatisticsOnSurfaceVariant(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    minTextSize = 8.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp) // Prevent descender clipping
                )

                // Optional secondary line (e.g. "best 3" / mastery %) — kept on both paired tiles so
                // they share line count and therefore height.
                sublabel?.let {
                    AutoSizeText(
                        text = it,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = getStatisticsOnSurfaceVariant(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        minTextSize = 8.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
