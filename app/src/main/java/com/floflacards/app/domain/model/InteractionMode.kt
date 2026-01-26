package com.floflacards.app.domain.model

/**
 * Defines the different interaction modes for the flashcard overlay
 * Following SOLID principles - Single Responsibility for mode definitions
 * 
 * This replaces the previous cycling behavior with explicit mode selection
 * Each mode has a distinct purpose and behavior:
 * - NORMAL: Default learning mode for flashcard interaction
 * - DRAG: Move flashcard position around the screen
 * - RESIZE: Adjust flashcard dimensions
 * - OPACITY: Control flashcard transparency (10%-100%)
 */
enum class InteractionMode(
    val icon: String
) {
    NORMAL(
        icon = "📖"
    ),
    DRAG(
        icon = "🎯"
    ),
    RESIZE(
        icon = "📏"
    ),
    OPACITY(
        icon = "👁️"
    );

    /**
     * Check if this mode allows flashcard content interaction
     */
    fun allowsContentInteraction(): Boolean = when (this) {
        NORMAL, OPACITY -> true
        DRAG, RESIZE -> false
    }

    /**
     * Get the primary color for this mode's visual indicators
     */
    fun getPrimaryColor(): Long = when (this) {
        NORMAL -> 0xFFFFFFFF
        DRAG -> 0xFF4CAF50    // Green
        RESIZE -> 0xFFFF9800  // Orange
        OPACITY -> 0xFF2196F3 // Blue
    }

    /**
     * Get display name for category replacement when in specific modes
     * Following KISS principle - simple mode indication without extra UI elements
     * Note: This now returns null for non-NORMAL modes since localized strings
     * need to be resolved in UI components with proper Context access
     */
    fun getCategoryDisplayName(originalCategoryName: String?): String? {
        return when (this) {
            NORMAL -> originalCategoryName ?: "Unknown Category"
            DRAG, RESIZE, OPACITY -> null // Will be resolved in UI components
        }
    }

    companion object {
        /**
         * Get all modes except NORMAL for modal selection
         */
        fun getSelectableModes(): List<InteractionMode> {
            return values().toList()
        }

        /**
         * Validate opacity value is within acceptable range
         */
        fun validateOpacity(opacity: Float): Float {
            return opacity.coerceIn(0.1f, 1.0f) // Minimum 10% opacity
        }
    }
}
