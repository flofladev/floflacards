package com.floflacards.app.presentation.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Shared styling constants to eliminate code duplication
 * Following DRY principle by centralizing common styling patterns
 * 
 * Note: This only consolidates TRUE duplicates, not intentional design differences.
 * Different color schemes for different contexts are preserved.
 */
object SharedStyles {
    
    /**
     * Common corner radius values used across components
     */
    object CornerRadius {
        val small = RoundedCornerShape(8.dp)     // Used for buttons, small cards
        val medium = RoundedCornerShape(12.dp)   // Used for medium cards, controls
        val large = RoundedCornerShape(16.dp)    // Used for content cards
        val xlarge = RoundedCornerShape(20.dp)   // Used for main container
    }
}

/**
 * Common button elevation patterns
 * These must be @Composable functions since ButtonDefaults requires Composable context
 */
object ButtonElevation {
    @Composable
    fun standard() = ButtonDefaults.buttonElevation(
        defaultElevation = 2.dp,
        pressedElevation = 0.dp
    )
    
    @Composable
    fun enhanced() = ButtonDefaults.buttonElevation(
        defaultElevation = 6.dp,
        pressedElevation = 2.dp
    )
}

/**
 * Common card elevation patterns
 * These must be @Composable functions since CardDefaults requires Composable context
 */
object CardElevation {
    @Composable
    fun low() = CardDefaults.cardElevation(defaultElevation = 2.dp)
    
    @Composable
    fun standard() = CardDefaults.cardElevation(defaultElevation = 4.dp)
    
    @Composable
    fun enhanced() = CardDefaults.cardElevation(defaultElevation = 6.dp)
    
    @Composable
    fun high() = CardDefaults.cardElevation(
        defaultElevation = 12.dp,
        pressedElevation = 8.dp
    )
}
