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

package com.floflacards.app.presentation.screen

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.AppTheme
import com.floflacards.app.presentation.component.WelcomeScreen
import com.floflacards.app.presentation.theme.FloatingLearningTheme
import com.floflacards.app.presentation.viewmodel.WelcomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : AppCompatActivity() {
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    
    @Inject
    lateinit var settingsManager: SettingsRepository
    
    // Permission launchers for handling system permission requests
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { 
        // Refresh permission state when user returns from settings
        welcomeViewModel?.refreshPermissions()
    }
    
    // SAF folder selection launcher
    private val safFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { treeUri ->
                // Take persistent permission for the selected folder
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                // Notify the ViewModel about the selected folder
                welcomeViewModel?.handleSafFolderSelected(treeUri.toString())
            }
        } else {
            // User cancelled folder selection - refresh to show current state
            welcomeViewModel?.refreshPermissions()
        }
    }
    
    
    private var welcomeViewModel: WelcomeViewModel? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up modern back navigation handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - users must complete the welcome flow
                // This ensures all permissions are granted before using the app
            }
        })
        
        // Check if welcome flow has been completed
        val isWelcomeCompleted = sharedPreferences.getBoolean("welcome_completed", false)
        
        // If welcome is completed and all permissions are still granted, skip to main app
        if (isWelcomeCompleted && areAllPermissionsGranted()) {
            navigateToMainApp()
            return
        }
        
        setContent {
            // Observe theme preference from SettingsRepository
            val currentTheme by settingsManager.appTheme.collectAsState()
            
            // Determine if dark theme should be used based on app theme setting
            val isDarkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            
            // Update system bars to match in-app theme (not device theme)
            LaunchedEffect(isDarkTheme) {
                updateSystemBars(isDarkTheme)
            }
            
            FloatingLearningTheme(appTheme = currentTheme) {
                val viewModel: WelcomeViewModel = hiltViewModel()
                welcomeViewModel = viewModel
                
                WelcomeScreen(
                    viewModel = viewModel,
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestBackupFolder = { requestBackupFolder() },
                    onWelcomeCompleted = { completeWelcomeFlow() },
                    onLanguageChanged = { selectedLanguage ->
                        // Apply the language change through SettingsRepository
                        settingsManager.setAppLocale(selectedLanguage)
                        // Recreate activity to apply the new locale
                        recreate()
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permissions when returning from system settings
        welcomeViewModel?.refreshPermissions()
    }
    

    
    private fun areAllPermissionsGranted(): Boolean {
        // Check if welcome screen has been completed and all permissions are still granted
        val isWelcomeCompleted = sharedPreferences.getBoolean("welcome_completed", false)
        if (!isWelcomeCompleted) {
            return false
        }
        
        // Use PermissionHelper to check all required permissions
        val permissionHelper = com.floflacards.app.util.PermissionHelper(this)
        val batteryOptSkipped = settingsManager.isBatteryOptimizationSkipped()
        val batteryOptEverDisabled = settingsManager.hasBatteryOptimizationEverBeenDisabled()
        
        val hasRequiredPermissions = permissionHelper.hasOverlayPermission()
        
        // Battery optimization logic:
        // 1. If user skipped: always skip welcome (user doesn't want to be bothered)
        // 2. If user disabled but system re-enabled: skip welcome (handle with contextual dialogs in main app)
        // 3. If fresh install and not disabled: show welcome
        val batteryOptRequirementMet = permissionHelper.isBatteryOptimizationDisabled() || 
                                       batteryOptSkipped || 
                                       batteryOptEverDisabled
        
        return hasRequiredPermissions && batteryOptRequirementMet
    }
    
    private fun requestOverlayPermission() {
        val intent = android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION.let {
            Intent(it, android.net.Uri.parse("package:$packageName"))
        }
        overlayPermissionLauncher.launch(intent)
    }
    
    /**
     * Requests SAF backup folder selection from the user.
     * Opens the system folder picker for persistent backup storage.
     */
    private fun requestBackupFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        safFolderLauncher.launch(intent)
    }
    
    
    private fun completeWelcomeFlow() {
        // Mark welcome as completed
        sharedPreferences.edit()
            .putBoolean("welcome_completed", true)
            .apply()
        
        // Navigate to main app
        navigateToMainApp()
    }
    
    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Prevent returning to welcome screen
    }
    
    /**
     * Updates system bars (status bar and navigation bar) to match the in-app theme.
     * This ensures system bars follow the app theme, not the device theme.
     */
    private fun updateSystemBars(isDarkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
                detectDarkMode = { isDarkTheme }
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
                detectDarkMode = { isDarkTheme }
            )
        )
    }
}
