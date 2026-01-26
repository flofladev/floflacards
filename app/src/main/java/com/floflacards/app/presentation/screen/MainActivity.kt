package com.floflacards.app.presentation.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.AppTheme
import com.floflacards.app.presentation.theme.FloatingLearningTheme
import com.floflacards.app.presentation.navigation.AppNavigation
import com.floflacards.app.util.PermissionLauncher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    // Permission handler for all permission-related functionality
    lateinit var permissionHandler: PermissionLauncher
    
    // Settings manager for theme preference
    @Inject
    lateinit var settingsManager: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize permission handler
        permissionHandler = PermissionLauncher(this)
        
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
                val navController = rememberNavController()
                
                // Use extracted navigation component
                AppNavigation(
                    navController = navController,
                    onRequestOverlayPermission = { permissionHandler.requestOverlayPermission() }
                )
            }
        }
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

// UI Components moved to separate file: MainScreenComponents.kt
// This follows Single Responsibility Principle and promotes reusability
// Components include:
// - ModernHeaderSection
// - StatusDashboard
// - ModernActionCard
// - ResponsiveActionCard
// - ResponsiveSettingsCard
// - NextFlashcardCountdownCard
