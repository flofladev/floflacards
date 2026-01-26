package com.floflacards.app.presentation.component

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.floflacards.app.util.PermissionHelper

/**
 * Permission management component following SRP.
 * Handles only permission-related state and logic.
 * Follows KISS principle with simple, focused API.
 */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    val permissionHelper = remember { PermissionHelper(context) }
    
    var hasOverlayPermission by remember { mutableStateOf(permissionHelper.hasOverlayPermission()) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    
    // Function to refresh permission state
    val refreshPermissionState = {
        hasOverlayPermission = permissionHelper.hasOverlayPermission()
    }
    
    return PermissionState(
        hasOverlayPermission = hasOverlayPermission,
        showOverlayPermissionDialog = showOverlayPermissionDialog,
        showBatteryOptimizationDialog = showBatteryOptimizationDialog,
        permissionHelper = permissionHelper,
        refreshPermissionState = refreshPermissionState,
        setShowOverlayPermissionDialog = { showOverlayPermissionDialog = it },
        setShowBatteryOptimizationDialog = { showBatteryOptimizationDialog = it }
    )
}

/**
 * Data class to hold permission state.
 * Follows data encapsulation principles.
 */
data class PermissionState(
    val hasOverlayPermission: Boolean,
    val showOverlayPermissionDialog: Boolean,
    val showBatteryOptimizationDialog: Boolean,
    val permissionHelper: PermissionHelper,
    val refreshPermissionState: () -> Unit,
    val setShowOverlayPermissionDialog: (Boolean) -> Unit,
    val setShowBatteryOptimizationDialog: (Boolean) -> Unit
)

/**
 * Permission dialogs using unified dialog system.
 * Eliminates code duplication from MainActivity.
 */
@Composable
fun PermissionDialogs(
    permissionState: PermissionState,
    onRequestOverlayPermission: () -> Unit
) {
    if (permissionState.showOverlayPermissionDialog) {
        ConfirmationDialog(
            title = "Overlay Permission Required",
            message = "This app needs overlay permission to show floating flashcards on top of other apps. Please grant the permission in the next screen.",
            confirmButtonText = "Grant Permission",
            onConfirm = {
                permissionState.setShowOverlayPermissionDialog(false)
                onRequestOverlayPermission()
            },
            onDismiss = { permissionState.setShowOverlayPermissionDialog(false) }
        )
    }
    
    if (permissionState.showBatteryOptimizationDialog) {
        ConfirmationDialog(
            title = "Battery Optimization",
            message = "For best performance, please disable battery optimization for this app. This ensures flashcards appear reliably.",
            confirmButtonText = "Disable Optimization",
            onConfirm = {
                permissionState.setShowBatteryOptimizationDialog(false)
                permissionState.permissionHelper.requestBatteryOptimizationDisable()
            },
            onDismiss = { permissionState.setShowBatteryOptimizationDialog(false) }
        )
    }
}
