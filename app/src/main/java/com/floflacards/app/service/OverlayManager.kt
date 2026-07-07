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
import android.content.res.Configuration
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
    private var keyboardDetector: KeyboardVisibilityDetector? = null

    // Visibility arbiter: the card is visible only while NO hide reason holds.
    // Each feature owns its flag; applyVisibility() combines them, so the
    // keyboard and landscape features can never fight over the same switch.
    private var hideInLandscapeEnabled = false
    private var hiddenByKeyboard = false
    private var hiddenByLandscape = false

    /**
     * True while the card is held hidden pending the keyboard probe's first
     * report. This is what prevents a card arriving mid-typing from flashing
     * for a frame before the probe catches up: the card is added already GONE
     * and only the report may reveal it.
     */
    private var awaitingKeyboardReport = false

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
        hideWhileTyping: Boolean = false,
        hideInLandscape: Boolean = false,
        content: @androidx.compose.runtime.Composable () -> Unit
    ): Boolean {
        try {
            val uiState = flashcardUiPreferences.getFlashcardUiState()

            // CRITICAL FIX: Ensure modal is not visible when starting a new overlay
            if (uiState.isModalVisible) {
                flashcardUiPreferences.saveModalVisible(false)
            }

            // Initialize the arbiter for this card. Landscape is known synchronously;
            // the keyboard needs the probe's first report.
            hideInLandscapeEnabled = hideInLandscape
            hiddenByLandscape = hideInLandscape && isLandscape()
            hiddenByKeyboard = false
            awaitingKeyboardReport = false

            // Start the probe BEFORE the card view exists so its first layout pass has
            // a head start; its callbacks are posted on the main looper, so nothing can
            // fire until after this method returns. Always tear down any prior probe
            // first: showOverlay may run again before closeOverlay (e.g. a new card
            // replacing a stuck one), and each probe is a real WindowManager.addView +
            // poll loop that would otherwise orphan.
            keyboardDetector?.stop()
            keyboardDetector = null
            if (hideWhileTyping) {
                val detector = KeyboardVisibilityDetector(context)
                val started = detector.start { keyboardVisible ->
                    hiddenByKeyboard = keyboardVisible
                    awaitingKeyboardReport = false
                    applyVisibility()
                }
                if (started) {
                    keyboardDetector = detector
                    awaitingKeyboardReport = true
                } else {
                    // Probe could not be added — fail open: a card that might flash
                    // over a keyboard beats a card that never appears.
                    Log.w(TAG, "Keyboard probe unavailable, showing card without it")
                }
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
                // Correct visibility from the first frame: the card is never added
                // visible only to be hidden a beat later.
                visibility = currentVisibility()
            }

            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay window created successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            return false
        }
    }

    private fun isLandscape(): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun currentVisibility(): Int =
        if (hiddenByKeyboard || hiddenByLandscape || awaitingKeyboardReport) View.GONE else View.VISIBLE

    private fun applyVisibility() {
        overlayView?.visibility = currentVisibility()
    }

    /**
     * Re-evaluates the landscape hide after an orientation change and re-applies
     * the combined visibility. Called by the service's onConfigurationChanged.
     */
    fun refreshVisibilityForOrientation() {
        hiddenByLandscape = hideInLandscapeEnabled && isLandscape()
        applyVisibility()
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
     * Re-applies the saved geometry for the current orientation to the live window.
     * Called on orientation change so a visible card snaps to that orientation's
     * remembered position/size instead of keeping stale pixels from the previous one.
     */
    fun refreshLayoutForCurrentOrientation() {
        overlayView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                val uiState = flashcardUiPreferences.getFlashcardUiState()
                params.x = uiState.positionX
                params.y = uiState.positionY
                params.width = uiState.width
                params.height = uiState.height
                windowManager?.updateViewLayout(view, params)
                Log.d(TAG, "Overlay layout refreshed for new orientation")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh layout for orientation", e)
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

        // Tear down the keyboard probe window immediately, alongside the card.
        keyboardDetector?.stop()
        keyboardDetector = null

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
        keyboardDetector?.stop()
        keyboardDetector = null
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
