package com.floflacards.app.data.source

import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import android.view.WindowManager
import com.floflacards.app.domain.model.InteractionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistent UI preferences for flashcard overlay
 * Handles position, size, and mode states with orientation support
 */
@Singleton
class FlashcardUiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "flashcard_ui_preferences"
        
        // Position preferences (as percentage of screen)
        private const val KEY_POSITION_X_PERCENT = "position_x_percent"
        private const val KEY_POSITION_Y_PERCENT = "position_y_percent"
        
        // Size preferences (as percentage of screen)
        private const val KEY_WIDTH_PERCENT = "width_percent"
        private const val KEY_HEIGHT_PERCENT = "height_percent"
        
        // Enhanced mode preferences - replacing boolean flags with single mode
        private const val KEY_CURRENT_MODE = "current_interaction_mode"
        private const val KEY_OPACITY = "flashcard_opacity"
        private const val KEY_IS_MODAL_VISIBLE = "is_mode_modal_visible"
        
        // Legacy mode preferences (kept for backward compatibility)
        private const val KEY_IS_DRAG_MODE_ENABLED = "is_drag_mode_enabled"
        private const val KEY_IS_RESIZE_MODE_ENABLED = "is_resize_mode_enabled"
        
        // Default values
        private const val DEFAULT_POSITION_X_PERCENT = 0.05f // 5% from left
        private const val DEFAULT_POSITION_Y_PERCENT = 0.2f  // 20% from top
        private const val DEFAULT_WIDTH_PERCENT = 0.9f       // 90% of screen width
        private const val DEFAULT_HEIGHT_PERCENT = 0.4f      // 40% of screen height
        
        // Constraints - Hybrid approach for better UX across devices
        private const val MIN_WIDTH_DP = 250f        // Minimum 200dp width (ensures usability)
        private const val MIN_HEIGHT_DP = 200f       // Minimum 200dp height (ensures all controls visible)
        private const val MAX_WIDTH_PERCENT = 0.95f  // Maximum 95% of screen width
        private const val MAX_HEIGHT_PERCENT = 0.8f  // Maximum 80% of screen height
    }
    
    /**
     * Enhanced data class representing flashcard UI state
     * Now supports the new modal-based interaction system
     */
    data class FlashcardUiState(
        val positionX: Int,
        val positionY: Int,
        val width: Int,
        val height: Int,
        val opacity: Float = 1.0f,
        val currentMode: InteractionMode = InteractionMode.NORMAL,
        val isModalVisible: Boolean = false,
        // Legacy properties for backward compatibility
        val isDragModeEnabled: Boolean = false,
        val isResizeModeEnabled: Boolean = false
    ) {
        /**
         * Helper method to check if we're in any edit mode
         */
        fun isInEditMode(): Boolean = currentMode != InteractionMode.NORMAL
        
        /**
         * Get the appropriate alpha value for the flashcard
         */
        fun getAlpha(): Float = InteractionMode.validateOpacity(opacity)
    }
    
    /**
     * Get current screen dimensions
     */
    private fun getScreenDimensions(): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        // Use modern API for getting display metrics
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            return Pair(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }
    
    /**
     * Convert percentage to pixels based on screen dimensions
     */
    private fun percentToPixels(percent: Float, dimension: Int): Int {
        return (percent * dimension).toInt()
    }
    
    /**
     * Convert pixels to percentage based on screen dimensions
     */
    private fun pixelsToPercent(pixels: Int, dimension: Int): Float {
        return if (dimension > 0) pixels.toFloat() / dimension else 0f
    }
    
    /**
     * Convert dp to pixels based on device density
     */
    private fun dpToPixels(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
    
    /**
     * Get current flashcard UI state with screen-relative positioning
     */
    fun getFlashcardUiState(): FlashcardUiState {
        val (screenWidth, screenHeight) = getScreenDimensions()
        
        val xPercent = prefs.getFloat(KEY_POSITION_X_PERCENT, DEFAULT_POSITION_X_PERCENT)
        val yPercent = prefs.getFloat(KEY_POSITION_Y_PERCENT, DEFAULT_POSITION_Y_PERCENT)
        val widthPercent = prefs.getFloat(KEY_WIDTH_PERCENT, DEFAULT_WIDTH_PERCENT)
        val heightPercent = prefs.getFloat(KEY_HEIGHT_PERCENT, DEFAULT_HEIGHT_PERCENT)
        
        // Convert percentages to pixels
        val positionX = percentToPixels(xPercent, screenWidth)
        val positionY = percentToPixels(yPercent, screenHeight)
        val width = percentToPixels(widthPercent, screenWidth)
        val height = percentToPixels(heightPercent, screenHeight)
        
        // Ensure flashcard stays within screen bounds
        val constrainedX = positionX.coerceIn(0, (screenWidth - width).coerceAtLeast(0))
        val constrainedY = positionY.coerceIn(0, (screenHeight - height).coerceAtLeast(0))
        
        // Get enhanced state properties
        val opacity = prefs.getFloat(KEY_OPACITY, 1.0f)
        val currentModeOrdinal = prefs.getInt(KEY_CURRENT_MODE, InteractionMode.NORMAL.ordinal)
        val currentMode = try {
            InteractionMode.values()[currentModeOrdinal]
        } catch (e: Exception) {
            InteractionMode.NORMAL // Fallback to normal if invalid mode
        }
        val isModalVisible = prefs.getBoolean(KEY_IS_MODAL_VISIBLE, false)
        
        return FlashcardUiState(
            positionX = constrainedX,
            positionY = constrainedY,
            width = width,
            height = height,
            opacity = InteractionMode.validateOpacity(opacity),
            currentMode = currentMode,
            isModalVisible = isModalVisible,
            // Legacy compatibility
            isDragModeEnabled = prefs.getBoolean(KEY_IS_DRAG_MODE_ENABLED, false),
            isResizeModeEnabled = prefs.getBoolean(KEY_IS_RESIZE_MODE_ENABLED, false)
        )
    }
    
    /**
     * Save flashcard position (converts to percentage for orientation independence)
     */
    fun savePosition(x: Int, y: Int) {
        val (screenWidth, screenHeight) = getScreenDimensions()
        val xPercent = pixelsToPercent(x, screenWidth)
        val yPercent = pixelsToPercent(y, screenHeight)
        
        prefs.edit()
            .putFloat(KEY_POSITION_X_PERCENT, xPercent)
            .putFloat(KEY_POSITION_Y_PERCENT, yPercent)
            .apply()
    }
    
    /**
     * Save flashcard size with hybrid constraints (dp minimums, percentage maximums)
     */
    fun saveSize(width: Int, height: Int) {
        val (screenWidth, screenHeight) = getScreenDimensions()
        
        // Apply dp-based minimums and percentage-based maximums
        val minWidthPx = dpToPixels(MIN_WIDTH_DP)
        val minHeightPx = dpToPixels(MIN_HEIGHT_DP)
        val maxWidthPx = percentToPixels(MAX_WIDTH_PERCENT, screenWidth)
        val maxHeightPx = percentToPixels(MAX_HEIGHT_PERCENT, screenHeight)
        
        val constrainedWidth = width.coerceIn(minWidthPx, maxWidthPx)
        val constrainedHeight = height.coerceIn(minHeightPx, maxHeightPx)
        
        // Convert to percentages for storage
        val widthPercent = pixelsToPercent(constrainedWidth, screenWidth)
        val heightPercent = pixelsToPercent(constrainedHeight, screenHeight)
        
        prefs.edit()
            .putFloat(KEY_WIDTH_PERCENT, widthPercent)
            .putFloat(KEY_HEIGHT_PERCENT, heightPercent)
            .apply()
    }
    
    /**
     * Save drag mode state
     */
    fun saveDragModeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_DRAG_MODE_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Save resize mode state
     */
    fun saveResizeModeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_RESIZE_MODE_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Get minimum size constraints in pixels (dp-based for consistent UX)
     */
    fun getMinSize(): Pair<Int, Int> {
        return Pair(
            dpToPixels(MIN_WIDTH_DP),
            dpToPixels(MIN_HEIGHT_DP)
        )
    }
    
    /**
     * Get maximum size constraints in pixels
     */
    fun getMaxSize(): Pair<Int, Int> {
        val (screenWidth, screenHeight) = getScreenDimensions()
        return Pair(
            percentToPixels(MAX_WIDTH_PERCENT, screenWidth),
            percentToPixels(MAX_HEIGHT_PERCENT, screenHeight)
        )
    }
    
    /**
     * Check if position and size are within screen bounds
     */
    fun isWithinBounds(x: Int, y: Int, width: Int, height: Int): Boolean {
        val (screenWidth, screenHeight) = getScreenDimensions()
        return x >= 0 && y >= 0 && 
               (x + width) <= screenWidth && 
               (y + height) <= screenHeight
    }
    
    /**
     * Constrain position and size to screen bounds
     */
    fun constrainToBounds(x: Int, y: Int, width: Int, height: Int): FlashcardUiState {
        val (screenWidth, screenHeight) = getScreenDimensions()
        val (minWidth, minHeight) = getMinSize()
        val (maxWidth, maxHeight) = getMaxSize()
        
        // Constrain size first
        val constrainedWidth = width.coerceIn(minWidth, maxWidth.coerceAtMost(screenWidth))
        val constrainedHeight = height.coerceIn(minHeight, maxHeight.coerceAtMost(screenHeight))
        
        // Then constrain position
        val constrainedX = x.coerceIn(0, (screenWidth - constrainedWidth).coerceAtLeast(0))
        val constrainedY = y.coerceIn(0, (screenHeight - constrainedHeight).coerceAtLeast(0))
        
        // Get current state for consistency
        val currentState = getFlashcardUiState()
        
        return FlashcardUiState(
            positionX = constrainedX,
            positionY = constrainedY,
            width = constrainedWidth,
            height = constrainedHeight,
            opacity = currentState.opacity,
            currentMode = currentState.currentMode,
            isModalVisible = currentState.isModalVisible,
            // Legacy compatibility
            isDragModeEnabled = prefs.getBoolean(KEY_IS_DRAG_MODE_ENABLED, false),
            isResizeModeEnabled = prefs.getBoolean(KEY_IS_RESIZE_MODE_ENABLED, false)
        )
    }
    
    // =====================================================
    // ENHANCED PREFERENCE METHODS - Following SOLID principles
    // =====================================================
    
    /**
     * Save current interaction mode
     */
    fun saveCurrentMode(mode: InteractionMode) {
        prefs.edit()
            .putInt(KEY_CURRENT_MODE, mode.ordinal)
            .apply()
        
        // Sync with legacy flags for backward compatibility
        when (mode) {
            InteractionMode.DRAG -> {
                saveDragModeEnabled(true)
                saveResizeModeEnabled(false)
            }
            InteractionMode.RESIZE -> {
                saveDragModeEnabled(false)
                saveResizeModeEnabled(true)
            }
            else -> {
                saveDragModeEnabled(false)
                saveResizeModeEnabled(false)
            }
        }
    }
    
    /**
     * Save flashcard opacity with validation
     */
    fun saveOpacity(opacity: Float) {
        val validatedOpacity = InteractionMode.validateOpacity(opacity)
        prefs.edit()
            .putFloat(KEY_OPACITY, validatedOpacity)
            .apply()
    }
    
    /**
     * Save modal visibility state
     */
    fun saveModalVisible(visible: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_MODAL_VISIBLE, visible)
            .apply()
    }
    
    /**
     * Get current opacity value
     */
    fun getCurrentOpacity(): Float {
        return InteractionMode.validateOpacity(prefs.getFloat(KEY_OPACITY, 1.0f))
    }
    
    /**
     * Get current interaction mode
     */
    fun getCurrentMode(): InteractionMode {
        val modeOrdinal = prefs.getInt(KEY_CURRENT_MODE, InteractionMode.NORMAL.ordinal)
        return try {
            InteractionMode.values()[modeOrdinal]
        } catch (e: Exception) {
            InteractionMode.NORMAL
        }
    }
    
    /**
     * Reset to default values - Enhanced version
     */
    fun resetToDefaults() {
        prefs.edit()
            .putFloat(KEY_POSITION_X_PERCENT, DEFAULT_POSITION_X_PERCENT)
            .putFloat(KEY_POSITION_Y_PERCENT, DEFAULT_POSITION_Y_PERCENT)
            .putFloat(KEY_WIDTH_PERCENT, DEFAULT_WIDTH_PERCENT)
            .putFloat(KEY_HEIGHT_PERCENT, DEFAULT_HEIGHT_PERCENT)
            .putFloat(KEY_OPACITY, 1.0f)
            .putInt(KEY_CURRENT_MODE, InteractionMode.NORMAL.ordinal)
            .putBoolean(KEY_IS_MODAL_VISIBLE, false)
            // Legacy compatibility
            .putBoolean(KEY_IS_DRAG_MODE_ENABLED, false)
            .putBoolean(KEY_IS_RESIZE_MODE_ENABLED, false)
            .apply()
    }
}
