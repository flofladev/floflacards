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

package com.floflacards.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.floflacards.app.data.source.FlashcardUiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages overlay window operations including creation, positioning, sizing, and cleanup.
 * Follows Single Responsibility Principle by separating overlay management from service logic.
 */
class OverlayManager(
    private val context: Context,
    private val flashcardUiPreferences: FlashcardUiPreferences
) {
    companion object {
        private const val TAG = "OverlayManager"
        private const val CLEANUP_DELAY_MS = 300L
    }
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isClosing = false
    
    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    /**
     * Creates and shows overlay window with specified content.
     * Follows KISS principle with clean parameter handling.
     */
    fun showOverlay(
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        content: @androidx.compose.runtime.Composable () -> Unit
    ): Boolean {
        try {
            val uiState = flashcardUiPreferences.getFlashcardUiState()
            
            // CRITICAL FIX: Ensure modal is not visible when starting a new overlay
            if (uiState.isModalVisible) {
                flashcardUiPreferences.saveModalVisible(false)
            }
            
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            val params = WindowManager.LayoutParams(
                uiState.width,
                uiState.height,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        0,
                PixelFormat.TRANSLUCENT
            ).apply {
                x = uiState.positionX
                y = uiState.positionY
                gravity = Gravity.TOP or Gravity.START
            }
            
            overlayView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setContent(content)
            }
            
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay window created successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            return false
        }
    }
    
    /**
     * Updates window position with relative offset (for dragging).
     * Follows DRY principle by centralizing position update logic.
     */
    fun updateWindowPositionRelative(deltaX: Int, deltaY: Int) {
        overlayView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                val newX = params.x + deltaX
                val newY = params.y + deltaY
                val constrainedState = flashcardUiPreferences.constrainToBounds(newX, newY, params.width, params.height)
                params.x = constrainedState.positionX
                params.y = constrainedState.positionY
                windowManager?.updateViewLayout(view, params)
                flashcardUiPreferences.savePosition(params.x, params.y)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update window position", e)
            }
        }
    }
    
    /**
     * Updates window size with relative change (for resizing).
     * Follows DRY principle by centralizing size update logic.
     */
    fun updateWindowSizeRelative(deltaWidth: Int, deltaHeight: Int) {
        overlayView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                val newWidth = params.width + deltaWidth
                val newHeight = params.height + deltaHeight
                val constrainedState = flashcardUiPreferences.constrainToBounds(params.x, params.y, newWidth, newHeight)
                params.width = constrainedState.width
                params.height = constrainedState.height
                params.x = constrainedState.positionX
                params.y = constrainedState.positionY
                windowManager?.updateViewLayout(view, params)
                flashcardUiPreferences.saveSize(params.width, params.height)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update window size", e)
            }
        }
    }
    
    /**
     * Closes overlay window with proper cleanup timing.
     * Follows SOLID principles with single responsibility for cleanup.
     */
    fun closeOverlay(serviceScope: CoroutineScope, onComplete: () -> Unit) {
        if (isClosing) {
            Log.w(TAG, "Already closing overlay")
            return
        }
        
        isClosing = true
        Log.d(TAG, "Closing overlay")
        
        serviceScope.launch {
            try {
                // Give time for any running animations to complete
                delay(CLEANUP_DELAY_MS)
                
                // Remove the view safely
                overlayView?.let { view ->
                    try {
                        windowManager?.removeView(view)
                        Log.d(TAG, "Overlay view removed")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing overlay view", e)
                    }
                    overlayView = null
                }
                
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Error in closeOverlay", e)
                // Force cleanup even if there's an error
                try {
                    overlayView?.let { view ->
                        windowManager?.removeView(view)
                    }
                } catch (removeError: Exception) {
                    Log.e(TAG, "Error in force remove", removeError)
                }
                overlayView = null
                onComplete()
            }
        }
    }
    
    /**
     * Emergency cleanup for service destruction.
     * Follows YAGNI principle - only what's needed for cleanup.
     */
    fun forceCleanup() {
        if (!isClosing) {
            overlayView?.let { view ->
                try {
                    windowManager?.removeView(view)
                    Log.d(TAG, "Force cleanup completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in force cleanup", e)
                }
            }
            overlayView = null
        }
    }
    
    /**
     * Checks if overlay is currently active.
     */
    fun isOverlayActive(): Boolean = overlayView != null && !isClosing
}
