package com.floflacards.app.data.model

/**
 * Enum representing the available theme options for the app.
 * 
 * CRITICAL: The app theme is completely independent from the device/system theme.
 * Only the in-app settings control the theme - never use isSystemInDarkTheme().
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Defines theme options only
 * - Open/Closed: Easy to extend with new themes
 * - Interface Segregation: Clean enum interface
 */
enum class AppTheme(val displayName: String) {
    /**
     * Light theme - forces light mode regardless of device setting
     */
    LIGHT("Light"),
    
    /**
     * Dark theme - forces dark mode regardless of device setting
     */
    DARK("Dark"),
    
    /**
     * System theme - follows device theme setting (default behavior)
     * Note: This will be the default to maintain current behavior
     */
    SYSTEM("System Default");
    
    companion object {
        /**
         * Default theme option to maintain current app behavior
         */
        val DEFAULT = SYSTEM
        
        /**
         * Convert string value back to enum, with fallback to default
         */
        fun fromString(value: String): AppTheme {
            return values().find { it.name == value } ?: DEFAULT
        }
    }
}
