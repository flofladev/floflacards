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
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Detects whether a soft keyboard (IME) is currently on screen, even when it belongs to a
 * *different* app behind our overlay.
 *
 * The flashcard overlay is a non-focusable [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * window, so the modern WindowInsets / OnApplyWindowInsetsListener APIs never fire for it directly.
 * The trick is a tiny invisible probe window that:
 *   - keeps FLAG_NOT_FOCUSABLE so it never steals input focus from the app being typed in, but
 *   - adds FLAG_ALT_FOCUSABLE_IM so it sits *behind* the IME layer and is therefore occluded /
 *     inset by the keyboard when it appears.
 *
 * We then read the keyboard state two ways (whichever fires first wins): the IME window insets,
 * and a visible-display-frame fallback. A short poll backs up the listeners because layout/insets
 * callbacks on a non-focusable overlay are unreliable across OEMs.
 *
 * Only requires the SYSTEM_ALERT_WINDOW permission the app already holds.
 */
class KeyboardVisibilityDetector(
    private val context: Context
) {
    companion object {
        private const val TAG = "KeyboardDetector"
        // Fraction of the full screen height that must be occluded (visible-frame fallback) for us
        // to treat the keyboard as visible. Above status + navigation bars, below any soft keyboard.
        private const val KEYBOARD_HEIGHT_RATIO = 0.15f
        private const val POLL_INTERVAL_MS = 250L
        // The visible-frame fallback is a heuristic (it also fires for split-screen dividers,
        // immersive apps and transient layout passes). Require this many consecutive positive
        // reads before hiding so a one-off glitch can't blink the card away. Releasing is instant.
        private const val FALLBACK_CONFIRMATIONS = 2
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var probeView: View? = null
    private var onChanged: ((Boolean) -> Unit)? = null

    /** Last reported state, so we only emit on change. */
    var isKeyboardVisible: Boolean = false
        private set

    /**
     * Whether the first evaluation has been reported. The first one is always
     * emitted, even when the state equals the "no keyboard" default: callers
     * start the card hidden and rely on that initial report to reveal it, which
     * is what prevents the card from flashing over an already-open keyboard.
     */
    private var hasReported = false

    /** Consecutive positive reads from the visible-frame fallback (main thread only). */
    private var fallbackHits = 0

    private val pollRunnable = object : Runnable {
        override fun run() {
            val view = probeView ?: return
            evaluate(view)
            view.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    /**
     * Adds the invisible probe window and starts reporting keyboard visibility changes.
     * Safe to call once per detector; call [stop] before reusing.
     *
     * @return true when the probe is running (reports will follow), false when it could
     *   not be added — callers waiting on the first report must then fail open.
     */
    fun start(onKeyboardVisibilityChanged: (Boolean) -> Unit): Boolean {
        if (probeView != null) return true
        onChanged = onKeyboardVisibilityChanged

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1px wide, full height, transparent, never touchable, never focusable.
        // FLAG_ALT_FOCUSABLE_IM places this (non-focusable) window *behind* the IME so the keyboard
        // actually occludes/insets it. ADJUST_RESIZE asks it to participate in IME insets.
        val params = WindowManager.LayoutParams(
            1,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val view = View(context)

        try {
            windowManager.addView(view, params)
            probeView = view

            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                evaluate(v)
                insets
            }
            view.viewTreeObserver.addOnGlobalLayoutListener { evaluate(view) }
            // Poll as a safety net for OEMs where the callbacks stay silent.
            view.postDelayed(pollRunnable, POLL_INTERVAL_MS)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add keyboard probe window", e)
            probeView = null
            onChanged = null
            return false
        }
    }

    private fun evaluate(view: View) {
        val imeInset = imeInsetBottom(view)
        val visibleNow = if (imeInset > 0) {
            // Reliable signal — act immediately.
            fallbackHits = 0
            true
        } else {
            // Fallback: compare the visible display frame against the real screen height.
            val occluded = try {
                val visibleRect = Rect()
                view.getWindowVisibleDisplayFrame(visibleRect)
                val screenHeight = fullScreenHeight()
                screenHeight > 0 && (screenHeight - visibleRect.bottom) > screenHeight * KEYBOARD_HEIGHT_RATIO
            } catch (e: Exception) {
                false
            }
            // Debounce the heuristic before hiding; release as soon as occlusion clears.
            fallbackHits = if (occluded) fallbackHits + 1 else 0
            fallbackHits >= FALLBACK_CONFIRMATIONS
        }

        if (!hasReported || visibleNow != isKeyboardVisible) {
            hasReported = true
            isKeyboardVisible = visibleNow
            onChanged?.invoke(visibleNow)
        }
    }

    private fun imeInsetBottom(view: View): Int {
        return try {
            ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.ime())
                ?.bottom ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun fullScreenHeight(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.height()
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
                metrics.heightPixels
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read screen height", e)
            0
        }
    }

    /** Removes the probe window and stops reporting. Safe to call multiple times. */
    fun stop() {
        probeView?.let { view ->
            try {
                view.removeCallbacks(pollRunnable)
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove keyboard probe window", e)
            }
        }
        probeView = null
        onChanged = null
        isKeyboardVisible = false
        hasReported = false
        fallbackHits = 0
    }
}
