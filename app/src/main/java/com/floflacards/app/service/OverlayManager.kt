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
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
        // Fallback width of the edge-glow strip; normally the caller passes the
        // width of the user's chosen glow intensity.
        private const val EDGE_CUE_WIDTH_DP = 32f
    }
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isClosing = false
    private var keyboardDetector: KeyboardVisibilityDetector? = null

    /**
     * A view disowned by closeOverlay but whose delayed removal has not run yet.
     * Tracked separately so forceCleanup (and a replacing showOverlay) can still
     * remove it — otherwise a window whose removal coroutine never runs (scope
     * already cancelled) would stay on screen forever.
     */
    private var pendingRemovalView: View? = null

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

    /**
     * True while the window is still the edge-glow strip, before
     * moveWindowToCardGeometry() snaps it to the card's saved spot.
     */
    private var inCuePhase = false

    /**
     * Strip width for the current card's cue, retained because the strip
     * geometry is recomputed on orientation changes mid-cue.
     */
    private var cueStripWidthDp = EDGE_CUE_WIDTH_DP

    /**
     * Compose-observable mirror of the arbiter's outcome: true while the window
     * is actually VISIBLE. The entrance cue keys its timeline off this so a card
     * arriving hidden (keyboard open, landscape) plays its glow when it is
     * revealed, not invisibly in the background.
     */
    private val cardVisibleState = mutableStateOf(false)
    val cardVisible: State<Boolean> get() = cardVisibleState

    /**
     * Which screen edge the cue glow bleeds from (true = left). A state, not a
     * one-shot value: an orientation change mid-cue can move the card's saved
     * spot to the other half of the screen, and the gradient must follow the
     * strip to the new edge.
     */
    private val cueFromLeftEdgeState = mutableStateOf(false)
    val cueFromLeftEdge: State<Boolean> get() = cueFromLeftEdgeState

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
        entranceCue: Boolean = false,
        entranceCueStripWidthDp: Float = EDGE_CUE_WIDTH_DP,
        content: @androidx.compose.runtime.Composable () -> Unit
    ): Boolean {
        try {
            val uiState = flashcardUiPreferences.getFlashcardUiState()

            // CRITICAL FIX: Ensure modal is not visible when starting a new overlay
            if (uiState.isModalVisible) {
                flashcardUiPreferences.saveModalVisible(false)
            }

            // Take over cleanly from any previous card: a new card can arrive while
            // an old window is still up (replacing a stuck one) or still in its
            // delayed teardown. Whatever is lingering gets removed NOW — an
            // overwritten reference would otherwise stay on screen forever — and
            // the closing state is cleared, or makeWindowTouchable() would refuse
            // to ever unlock the new card.
            removePendingView()
            overlayView?.let { stale ->
                try {
                    windowManager?.removeView(stale)
                    Log.w(TAG, "Removed stale overlay window before showing new card")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing stale overlay window", e)
                }
                overlayView = null
            }
            isClosing = false

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

            // During the entrance cue the window must not eat touches: the glow is a
            // signal, not content, so a touchable window there would be an invisible
            // dead zone over the app underneath. EntranceCue flips this back via
            // makeWindowTouchable() once the card is actually visible.
            val touchFlag = if (entranceCue) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

            inCuePhase = entranceCue
            cueStripWidthDp = entranceCueStripWidthDp

            val params = WindowManager.LayoutParams(
                uiState.width,
                uiState.height,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        touchFlag,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (entranceCue) {
                    // The window opens as a slim strip hugging the screen edge nearest
                    // the card, mirroring the card's vertical extent — the glow also
                    // says WHERE the card will land. moveWindowToCardGeometry() snaps
                    // it to the card's real spot for the reveal.
                    applyCueStripGeometry(this, uiState)
                } else {
                    applyCardGeometry(this, uiState)
                }
            }

            val initialVisibility = currentVisibility()
            overlayView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setContent(content)
                // Correct visibility from the first frame: the card is never added
                // visible only to be hidden a beat later.
                visibility = initialVisibility
            }
            cardVisibleState.value = initialVisibility == View.VISIBLE

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

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun screenWidthPx(): Int {
        val wm = windowManager ?: return context.resources.displayMetrics.widthPixels
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels
        }
    }

    private fun isCardOnLeftHalf(uiState: FlashcardUiPreferences.FlashcardUiState): Boolean =
        uiState.positionX + uiState.width / 2 < screenWidthPx() / 2

    /** Applies the card's saved position and size to the given window params. */
    private fun applyCardGeometry(
        params: WindowManager.LayoutParams,
        uiState: FlashcardUiPreferences.FlashcardUiState
    ) {
        params.x = uiState.positionX
        params.y = uiState.positionY
        params.width = uiState.width
        params.height = uiState.height
    }

    /**
     * Applies the entrance-cue strip geometry: a slim strip hugging the screen
     * edge nearest the card, mirroring the card's vertical extent. Also updates
     * [cueFromLeftEdge] so the glow gradient always points away from the edge
     * the strip actually sits on.
     */
    private fun applyCueStripGeometry(
        params: WindowManager.LayoutParams,
        uiState: FlashcardUiPreferences.FlashcardUiState
    ) {
        val fromLeft = isCardOnLeftHalf(uiState)
        cueFromLeftEdgeState.value = fromLeft
        val stripWidth = dpToPx(cueStripWidthDp)
        params.width = stripWidth
        params.height = uiState.height
        params.x = if (fromLeft) 0 else screenWidthPx() - stripWidth
        params.y = uiState.positionY
    }

    private fun currentVisibility(): Int =
        if (hiddenByKeyboard || hiddenByLandscape || awaitingKeyboardReport) View.GONE else View.VISIBLE

    private fun applyVisibility() {
        val visibility = currentVisibility()
        overlayView?.visibility = visibility
        cardVisibleState.value = visibility == View.VISIBLE
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
     * Ends the cue phase: snaps the window from the edge strip to the card's
     * saved geometry. Reads the state fresh, so a rotation during the cue still
     * lands the card on the right orientation's spot. Touch stays off until
     * makeWindowTouchable().
     */
    fun moveWindowToCardGeometry() {
        inCuePhase = false
        if (isClosing) return
        overlayView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                applyCardGeometry(params, flashcardUiPreferences.getFlashcardUiState())
                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move window to card geometry", e)
            }
        }
    }

    /**
     * Clears FLAG_NOT_TOUCHABLE once the entrance cue has handed over to the real
     * card. Until then taps pass through to the app underneath; from here on the
     * card takes them. Safe to call late: if the overlay is already gone (closed
     * mid-cue) this is a no-op.
     */
    fun makeWindowTouchable() {
        if (isClosing) return
        overlayView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                if (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0) return
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to make window touchable", e)
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
                // Mid-cue the window is the edge strip, not the card, so refresh
                // THAT for the new orientation — the card's saved spot (and with
                // it the nearest edge) is per-orientation. The reveal reads fresh
                // card geometry itself.
                if (inCuePhase) {
                    applyCueStripGeometry(params, uiState)
                } else {
                    applyCardGeometry(params, uiState)
                }
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
        inCuePhase = false
        cardVisibleState.value = false

        // Disown the view NOW: if a new card's showOverlay runs before the
        // delayed removal below, it must not be able to touch the new window.
        pendingRemovalView = overlayView
        overlayView = null

        serviceScope.launch {
            try {
                // Give time for any running animations to complete
                delay(CLEANUP_DELAY_MS)
            } finally {
                // Runs even when the scope is cancelled mid-delay (service
                // destruction): the window must come down regardless.
                removePendingView()
                onComplete()
            }
        }
    }

    /** Removes the view a closeOverlay disowned, if it is still up. Idempotent. */
    private fun removePendingView() {
        pendingRemovalView?.let { view ->
            try {
                windowManager?.removeView(view)
                Log.d(TAG, "Overlay view removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
        }
        pendingRemovalView = null
    }

    /**
     * Emergency cleanup for service destruction. Removes both the live window
     * and any window whose delayed removal never got to run (the service scope
     * is cancelled before this is called).
     */
    fun forceCleanup() {
        keyboardDetector?.stop()
        keyboardDetector = null
        inCuePhase = false
        cardVisibleState.value = false
        removePendingView()
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
