package com.floflacards.app.presentation.component.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurrentStreakCard(
            streakDays = stats.streakDays,
            modifier = Modifier.weight(1f)
        )
        HighestStreakCard(
            highestStreak = stats.highestStreak,
            modifier = Modifier.weight(1f)
        )
        MasteredCard(
            mastered = stats.masteredFlashcards,
            total = stats.totalFlashcards,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun ModernCategoryCard(
    categoryStats: CategoryStats,
    onToggleExpansion: () -> Unit,
    onFlashcardResetClick: (FlashcardStats) -> Unit,
    onCategoryResetClick: () -> Unit
) {
    val isExpanded = categoryStats.isExpanded
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getStatisticsSurface()),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Category header - always clickable
            CategoryHeader(
                categoryStats = categoryStats,
                isExpanded = isExpanded,
                onToggleExpansion = onToggleExpansion,
                onCategoryResetClick = onCategoryResetClick
            )
            
            // Expandable flashcards list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(categoryStats.flashcards) { flashcard ->
                        FlashcardStatItem(
                            flashcard = flashcard,
                            onResetClick = { onFlashcardResetClick(flashcard) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    categoryStats: CategoryStats,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onCategoryResetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpansion() }
            .padding(12.dp),
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
                color = getStatisticsOnSurface()
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
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.stats_collapse_description) else stringResource(R.string.stats_expand_description),
                tint = getStatisticsOnSurfaceVariant()
            )
        }
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
private fun CurrentStreakCard(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    StatCard(
        value = "$streakDays",
        label = stringResource(R.string.stats_current_streak),
        accentColor = getStreakAccentColor(),
        backgroundColor = getStreakAccentBackground(),
        icon = {
            Text(
                text = if (streakDays > 0) "🔥" else "💤",
                fontSize = 14.sp
            )
        },
        modifier = modifier
    )
}

@Composable
private fun HighestStreakCard(
    highestStreak: Int,
    modifier: Modifier = Modifier
) {
    StatCard(
        value = "$highestStreak",
        label = stringResource(R.string.stats_highest_streak),
        accentColor = getBestAccentColor(),
        backgroundColor = getBestAccentBackground(),
        icon = {
            Text(
                text = if (highestStreak > 0) "🏆" else "🎯",
                fontSize = 14.sp
            )
        },
        modifier = modifier
    )
}

@Composable
private fun MasteredCard(
    mastered: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    StatCard(
        value = "$mastered/$total",
        label = stringResource(R.string.stats_total_flashcards),
        accentColor = getMasteryAccentColor(),
        backgroundColor = getMasteryAccentBackground(),
        icon = {
            Text(
                text = if (mastered > 0) "⭐" else "📚",
                fontSize = 14.sp
            )
        },
        progressBar = null,
        modifier = modifier
    )
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    accentColor: Color,
    backgroundColor: Color,
    icon: (@Composable () -> Unit)? = null,
    progressBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Adaptive height based on content
    val minHeight = if (progressBar != null) 110.dp else 90.dp
    
    Card(
        modifier = modifier
            .wrapContentHeight()
            .heightIn(min = minHeight),
        colors = CardDefaults.cardColors(containerColor = getStatisticsCardBackground()),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = getStatisticsCardBorder(),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 10.dp) // Slightly more vertical padding
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
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
                
                // Progress bar if provided
                progressBar?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    it.invoke()
                }
            }
        }
    }
}
