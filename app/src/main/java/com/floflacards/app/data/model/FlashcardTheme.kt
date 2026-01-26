package com.floflacards.app.data.model

/**
 * Enum representing the available theme options for flashcard overlay.
 * 
 * CRITICAL: The flashcard theme is completely independent from both the app theme and device theme.
 * This allows users to have different themes for the main app and flashcard overlay.
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Defines flashcard theme options only
 * - Open/Closed: Easy to extend with new themes
 * - Interface Segregation: Clean enum interface
 */
enum class FlashcardTheme(val displayName: String) {
    /**
     * Default theme - uses the current hardcoded flashcard design
     * Dark background, grayish questions, purple answers
     */
    DEFAULT("Default"),
    
    /**
     * Light theme - light background with dark text for better visibility
     */
    LIGHT("Light"),
    
    /**
     * Dark theme - enhanced dark mode with improved contrast
     */
    DARK("Dark");
    
    companion object {
        /**
         * Default theme option to maintain current flashcard behavior
         */
        val DEFAULT_THEME = DEFAULT
        
        /**
         * Convert string value back to enum, with fallback to default
         */
        fun fromString(value: String): FlashcardTheme {
            return values().find { it.name == value } ?: DEFAULT_THEME
        }
    }
}
