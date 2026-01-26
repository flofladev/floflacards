package com.floflacards.app.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.floflacards.app.data.model.FlashcardTheme

/**
 * Centralized flashcard color scheme following DRY principle.
 * Supports multiple themes: DEFAULT (current design), LIGHT, and DARK.
 * 
 * CRITICAL: Flashcard theme is independent from both app theme and device theme.
 * This eliminates hardcoded Color() values throughout the codebase and ensures
 * consistency across all flashcard components following SOLID principles.
 */
object FlashcardColors {
    
    /**
     * Theme color data class for clean organization
     */
    private data class ThemeColors(
        val darkBackground: Color,
        val headerBackground: Color,
        val questionBackground: Color,
        val questionText: Color,
        val answerBackground: Color,
        val answerText: Color,
        val buttonAccent: Color,
        val softWhite: Color
    )
    
    // DEFAULT theme colors - Refined dark theme with elegant purple accents
    private val defaultTheme = ThemeColors(
        darkBackground = Color(0xFF121212), // Rich dark background
        headerBackground = Color(0xFF1E1E1E), // Subtle contrast for header
        questionBackground = Color(0xFF2A2A2A), // Warmer gray for questions - less harsh
        questionText = Color(0xFFE6E1E5), // Soft light text with purple undertone
        answerBackground = Color(0xFF6750A4), // Modern purple - sophisticated
        answerText = Color(0xFFEADDFF), // Light purple tint - elegant
        buttonAccent = Color(0xFF7F39FB), // Vibrant purple accent - eye-catching
        softWhite = Color(0xFFE6E1E5) // Consistent soft light text
    )
    
    // LIGHT theme colors - Modern, elegant design with sophisticated purple palette
    private val lightTheme = ThemeColors(
        darkBackground = Color(0xFFFBFBFE), // Subtle off-white background for warmth
        headerBackground = Color(0xFFEDE7F6), // Soft lavender header - elegant and calming
        questionBackground = Color(0xFFF5F3FF), // Ultra-light purple with hint of blue - modern
        questionText = Color(0xFF2D1B69), // Rich deep purple - excellent readability
        answerBackground = Color(0xFFE8DEF8), // Sophisticated light purple - premium feel
        answerText = Color(0xFF1D192B), // Near-black with purple undertone - sharp contrast
        buttonAccent = Color(0xFF6750A4), // Refined purple - modern Material You style
        softWhite = Color(0xFF1C1B1F) // Rich dark text for perfect contrast
    )
    
    // DARK theme colors - Premium deep dark with luxurious purple accents
    private val darkTheme = ThemeColors(
        darkBackground = Color(0xFF0F0D13), // Deep purple-black - premium feel
        headerBackground = Color(0xFF1D1B20), // Rich dark with purple undertone
        questionBackground = Color(0xFF2B2930), // Sophisticated dark gray with warmth
        questionText = Color(0xFFE6E0E9), // Soft light text - easy on eyes
        answerBackground = Color(0xFF4F378B), // Deep rich purple - luxurious
        answerText = Color(0xFFD0BCFF), // Light lavender - premium accent
        buttonAccent = Color(0xFF7F39FB), // Bright purple - striking contrast
        softWhite = Color(0xFFE6E0E9) // Consistent refined light text
    )
    
    /**
     * Get theme-specific colors based on FlashcardTheme
     */
    private fun getThemeColors(theme: FlashcardTheme): ThemeColors = when (theme) {
        FlashcardTheme.DEFAULT -> defaultTheme
        FlashcardTheme.LIGHT -> lightTheme
        FlashcardTheme.DARK -> darkTheme
    }
    
    // Backward compatibility - maintain existing API using DEFAULT theme
    val DarkBackground get() = defaultTheme.darkBackground
    val HeaderBackground get() = defaultTheme.headerBackground
    val QuestionBackground get() = defaultTheme.questionBackground
    val QuestionText get() = defaultTheme.questionText
    val AnswerBackground get() = defaultTheme.answerBackground
    val AnswerText get() = defaultTheme.answerText
    val ButtonAccent get() = defaultTheme.buttonAccent
    val SoftWhite get() = defaultTheme.softWhite
    
    /**
     * Get question card colors for the overlay flashcards with theme support
     */
    @Composable
    fun getQuestionCardColors(
        theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
        isEnabled: Boolean = true
    ): androidx.compose.material3.CardColors {
        val themeColors = getThemeColors(theme)
        return androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                themeColors.questionBackground 
            else 
                themeColors.questionBackground.copy(alpha = 0.5f)
        )
    }
    
    /**
     * Get answer card colors for the overlay flashcards with theme support
     */
    @Composable
    fun getAnswerCardColors(
        theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
        isEnabled: Boolean = true
    ): androidx.compose.material3.CardColors {
        val themeColors = getThemeColors(theme)
        return androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                themeColors.answerBackground 
            else 
                themeColors.answerBackground.copy(alpha = 0.5f)
        )
    }
    
    /**
     * Get header background colors based on interaction mode with theme support
     */
    fun getHeaderBackgroundColor(
        theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
        mode: String = "NORMAL"
    ): Color {
        val themeColors = getThemeColors(theme)
        return when (mode) {
            "DRAG" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
            "RESIZE" -> Color(0xFFFF9800).copy(alpha = 0.15f)
            "OPACITY" -> Color(0xFF2196F3).copy(alpha = 0.15f)
            else -> themeColors.headerBackground
        }
    }
    
    /**
     * Backward compatibility overload - maintains existing API
     */
    fun getHeaderBackgroundColor(mode: String): Color {
        return getHeaderBackgroundColor(FlashcardTheme.DEFAULT_THEME, mode)
    }
    
    /**
     * Get text color based on enabled state for overlay flashcards with theme support
     */
    fun getTextColor(
        theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME,
        isEnabled: Boolean = true, 
        alpha: Float = 1f
    ): Color {
        val themeColors = getThemeColors(theme)
        val baseColor = themeColors.softWhite
        return if (isEnabled) baseColor.copy(alpha = alpha) else baseColor.copy(alpha = alpha * 0.6f)
    }
    
    /**
     * Get button colors for show answer button with theme support
     * Always uses white text for better contrast on colored button background
     */
    @Composable
    fun getShowAnswerButtonColors(
        theme: FlashcardTheme = FlashcardTheme.DEFAULT_THEME
    ): androidx.compose.material3.ButtonColors {
        val themeColors = getThemeColors(theme)
        return androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = themeColors.buttonAccent,
            contentColor = Color.White // Always white text for button contrast
        )
    }
    
    /**
     * Get theme-specific background color
     */
    fun getBackgroundColor(theme: FlashcardTheme): Color {
        val themeColors = getThemeColors(theme)
        return themeColors.darkBackground
    }
    
    /**
     * Get theme-specific question text color
     */
    fun getQuestionTextColor(theme: FlashcardTheme): Color {
        val themeColors = getThemeColors(theme)
        return themeColors.questionText
    }
    
    /**
     * Get theme-specific answer text color
     */
    fun getAnswerTextColor(theme: FlashcardTheme): Color {
        val themeColors = getThemeColors(theme)
        return themeColors.answerText
    }
}
