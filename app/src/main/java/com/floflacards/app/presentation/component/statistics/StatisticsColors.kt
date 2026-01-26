package com.floflacards.app.presentation.component.statistics

// Removed isSystemInDarkTheme import - using MaterialTheme.colorScheme instead
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme colors for statistics screens
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkOnSurface = Color(0xFFE1E1E1)
val DarkOnSurfaceVariant = Color(0xFFB3B3B3)
val AccentPurple = Color(0xFF9C27B0)
val AccentTeal = Color(0xFF00BCD4)
val AccentAmber = Color(0xFFFFC107)
val AccentGreen = Color(0xFF4CAF50)
val AccentRed = Color(0xFFF44336)

// Minimalistic card colors - subtle and muted
val CardBackground = Color(0xFF252525)
val CardBorder = Color(0xFF3A3A3A)

// Subtle accent colors for cards
val StreakAccent = Color(0xFF8E7CC3)        // Muted purple for current streak
val StreakAccentBg = Color(0xFF2A2438)      // Very subtle purple background
val BestAccent = Color(0xFF7FB069)          // Muted green for highest streak  
val BestAccentBg = Color(0xFF252B23)        // Very subtle green background
val MasteryAccent = Color(0xFFD4A574)       // Muted amber for mastery
val MasteryAccentBg = Color(0xFF2B2722)     // Very subtle amber background

// Progress colors
val ProgressBackground = Color(0xFF383838)
val ProgressFill = Color(0xFF7FB069)

/**
 * Theme-aware color functions for statistics screen
 * Provides proper light theme support while preserving dark theme appearance
 */

@Composable
fun getStatisticsBackground(): Color {
    // Use theme-aware background color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.background
}

@Composable
fun getStatisticsSurface(): Color {
    // Use theme-aware surface color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.surface
}

@Composable
fun getStatisticsSurfaceVariant(): Color {
    // Use theme-aware surface variant color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun getStatisticsOnSurface(): Color {
    // Use theme-aware on surface color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onSurface
}

@Composable
fun getStatisticsOnSurfaceVariant(): Color {
    // Use theme-aware on surface variant color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun getStatisticsCardBackground(): Color {
    // Use theme-aware surface variant color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun getStatisticsCardBorder(): Color {
    // Use theme-aware outline color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.outline
}

@Composable
fun getStatisticsProgressBackground(): Color {
    // Use theme-aware outline color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.outline
}

@Composable
fun getStatisticsProgressFill(): Color {
    // Use consistent accent green for progress in both themes
    return AccentGreen
}

// Streak card colors
@Composable
fun getStreakAccentColor(): Color {
    // Use white in dark mode, primary color in light mode for better contrast
    return MaterialTheme.colorScheme.onSurface
}

@Composable
fun getStreakAccentBackground(): Color {
    // Use theme-aware primary container color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.primaryContainer
}

// Best streak card colors
@Composable
fun getBestAccentColor(): Color {
    // Use consistent accent green for best streak in both themes
    return AccentGreen
}

@Composable
fun getBestAccentBackground(): Color {
    // Use theme-aware secondary container color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.secondaryContainer
}

// Mastery card colors
@Composable
fun getMasteryAccentColor(): Color {
    // Use theme-aware on surface color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.onSurface
}

@Composable
fun getMasteryAccentBackground(): Color {
    // Use theme-aware tertiary container color - automatically adapts to light/dark theme
    return MaterialTheme.colorScheme.tertiaryContainer
}
