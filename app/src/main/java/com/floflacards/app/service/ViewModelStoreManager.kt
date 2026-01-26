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
