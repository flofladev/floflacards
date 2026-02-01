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

import androidx.lifecycle.ViewModelStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for ViewModelStore to optimize memory usage in OverlayService.
 * Instead of creating new ViewModelStore for each overlay instance, reuses a single one.
 * 
 * This follows the SOLID principles:
 * - Single Responsibility: Only manages ViewModelStore lifecycle
 * - Open/Closed: Can be extended without modification
 * - Interface Segregation: Simple, focused interface
 * - Dependency Inversion: Can be injected and mocked
 */
@Singleton
class ViewModelStoreManager @Inject constructor() {
    
    private var overlayViewModelStore: ViewModelStore? = null
    private var referenceCount = 0
    
    /**
     * Gets the shared ViewModelStore for overlay services.
     * Creates a new one if none exists.
     * Thread-safe for multiple overlay instances.
     */
    @Synchronized
    fun getOverlayViewModelStore(): ViewModelStore {
        if (overlayViewModelStore == null) {
            overlayViewModelStore = ViewModelStore()
        }
        referenceCount++
        return overlayViewModelStore!!
    }
    
    /**
     * Releases reference to the ViewModelStore.
     * Clears the store when no more references exist.
     * This prevents memory leaks while allowing reuse.
     */
    @Synchronized
    fun releaseOverlayViewModelStore() {
        referenceCount--
        if (referenceCount <= 0) {
            overlayViewModelStore?.clear()
            overlayViewModelStore = null
            referenceCount = 0
        }
    }
    
    /**
     * Forces cleanup of the ViewModelStore.
     * Used when app is shutting down or in emergency cleanup.
     */
    @Synchronized
    fun forceCleanup() {
        overlayViewModelStore?.clear()
        overlayViewModelStore = null
        referenceCount = 0
    }
}
