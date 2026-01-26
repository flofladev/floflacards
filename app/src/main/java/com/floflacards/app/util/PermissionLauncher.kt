package com.floflacards.app.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Permission launcher component that manages permission request activities.
 * Extracted from MainActivity to follow Single Responsibility Principle.
 * Handles overlay permissions with proper callbacks.
 * Note: Storage permissions are not needed when using SAF (Storage Access Framework).
 */
class PermissionLauncher(private val activity: ComponentActivity) {
    
    // Shared permission state that gets updated when user returns from settings
    var permissionUpdateCallback: (() -> Unit)? = null
    
    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> = 
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { 
            // Update permission state when user returns from system settings
            permissionUpdateCallback?.invoke()
        }
    
    /**
     * Requests overlay permission from the system.
     * Maintains exact same functionality as original implementation.
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
}
