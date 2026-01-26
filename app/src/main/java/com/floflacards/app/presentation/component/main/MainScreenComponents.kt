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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.floflacards.app.presentation.component.getLightModeOnlyBorder
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.presentation.component.text.AutoSizeText

/**
 * Main screen UI components extracted from MainActivity.
 * Follows Single Responsibility Principle and promotes reusability.
 * Each component has a focused purpose and clean interface.
 */

/**
 * Modern header section with app branding and description.
 * Provides consistent visual identity across the app.
 * Fixed for proper light mode visibility.
 */
@Composable
fun ModernHeaderSection(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = getHeaderContainerColor()
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = getLightModeOnlyBorder(true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_header_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = getHeaderContentColor(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.home_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = getHeaderContentColor().copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Status dashboard showing learning service state and statistics.
 * Displays key metrics in a clean, organized layout.
 */
@Composable
fun StatusDashboard(
    isServiceActive: Boolean,
    activeFlashcardCount: Int,
    nextFlashcardCountdown: Long,
    streak: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ModernLearningStatusGrid(
                isServiceActive = isServiceActive,
                activeFlashcardCount = activeFlashcardCount,
                streak = streak,
                modifier = modifier
            )
            
            if (isServiceActive) {
                NextFlashcardCountdownCard(
                    countdownSeconds = nextFlashcardCountdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Modern action card with consistent styling and accessibility.
 * Provides primary and secondary variants for visual hierarchy.
 */
@Composable
fun ModernActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(120.dp)
            .semantics {
                this.contentDescription = contentDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            // Title
            AutoSizeText(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isPrimary) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minTextSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPrimary) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Responsive action card that adapts to different screen sizes.
 * Follows DRY principle by reusing existing card patterns with adaptive sizing.
 */
@Composable
fun ResponsiveActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    contentDescription: String,
    cardHeight: Dp,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(cardHeight)
            .semantics {
                this.contentDescription = contentDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon - adaptive size
            Text(
                text = icon,
                style = if (isCompact) 
                    MaterialTheme.typography.titleLarge 
                else 
                    MaterialTheme.typography.headlineMedium
            )
            
            // Title - adaptive typography with auto-sizing
            AutoSizeText(
                text = title,
                style = if (isCompact) 
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) 
                else 
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isPrimary) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minTextSize = if (isCompact) 10.sp else 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Subtitle - adaptive and optional for very compact screens
            if (!isCompact) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrimary) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Responsive settings card with dark styling that adapts to different screen sizes.
 * Follows app's design consistency while being adaptive.
 */
@Composable
fun ResponsiveSettingsCard(
    onClick: () -> Unit,
    cardHeight: Dp,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val settingsDescription = stringResource(R.string.home_settings_description)
    
    Card(
        onClick = onClick,
        modifier = modifier
            .height(cardHeight)
            .semantics {
                contentDescription = settingsDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = getSettingsCardColor() // Theme-aware settings card color
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon - adaptive size
            Text(
                text = "⚙️",
                style = if (isCompact) 
                    MaterialTheme.typography.titleLarge 
                else 
                    MaterialTheme.typography.headlineMedium
            )
            
            // Title - adaptive typography with auto-sizing
            AutoSizeText(
                text = stringResource(R.string.home_settings_title),
                style = if (isCompact) 
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) 
                else 
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minTextSize = if (isCompact) 10.sp else 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Subtitle - adaptive and optional for very compact screens
            if (!isCompact) {
                Text(
                    text = stringResource(R.string.settings_preferences),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Next flashcard countdown card showing time until next flashcard.
 * Provides clear visual feedback about learning session progress.
 */
@Composable
fun NextFlashcardCountdownCard(
    countdownSeconds: Long,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏰",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (countdownSeconds > 0) {
                    stringResource(R.string.main_next_flashcard_in, countdownSeconds)
                } else {
                    stringResource(R.string.main_preparing_next)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
