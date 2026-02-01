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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.presentation.viewmodel.MainViewModel
import com.floflacards.app.presentation.component.IntervalSelectionDialog
import com.floflacards.app.presentation.component.PermissionDialogs
import com.floflacards.app.presentation.component.rememberPermissionState
import com.floflacards.app.presentation.component.LearningControls
import com.floflacards.app.presentation.component.*
import com.floflacards.app.service.OverlayService
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

/**
 * Main screen composable that displays the primary app interface.
 * Extracted from MainActivity to follow Single Responsibility Principle.
 * Handles UI state, responsive design, and user interactions.
 */
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAppSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionState = rememberPermissionState()
    var showIntervalSelectionDialog by remember { mutableStateOf(false) }
    
    // Responsive design utilities
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current
    
    // Screen size breakpoints for adaptive layout
    val isCompactScreen = screenWidth < 360.dp
    val isSmallScreen = screenWidth < 480.dp
    val isMediumScreen = screenWidth < 600.dp
    val isLargeScreen = screenWidth >= 600.dp
    
    // Adaptive dimensions based on screen size
    val cardHeight = when {
        isCompactScreen -> 100.dp
        isSmallScreen -> 110.dp
        else -> 120.dp
    }
    
    val cardSpacing = when {
        isCompactScreen -> 8.dp
        isSmallScreen -> 10.dp
        else -> 12.dp
    }
    
    val contentPadding = when {
        isCompactScreen -> 12.dp
        isSmallScreen -> 14.dp
        else -> 16.dp
    }
    
    // Set up permission update callback
    DisposableEffect(Unit) {
        val activity = context as MainActivity
        activity.permissionHandler.permissionUpdateCallback = permissionState.refreshPermissionState
        onDispose {
            activity.permissionHandler.permissionUpdateCallback = null
        }
    }
    
    // Update permission state when activity resumes
    LaunchedEffect(Unit) {
        permissionState.refreshPermissionState()
        
        // Storage permission is now handled in Welcome Screen
        // No need to check here - users must complete welcome flow first
        
        // Check if we need to show overlay permission dialog
        if (!permissionState.hasOverlayPermission) {
            // Don't show dialog immediately - let user interact first
        } else if (!permissionState.permissionHelper.isBatteryOptimizationDisabled()) {
            // Show battery optimization dialog if:
            // 1. User hasn't skipped it during welcome flow, OR
            // 2. User previously disabled it but system re-enabled it (they want the feature)
            val hasSkipped = viewModel.isBatteryOptimizationSkipped()
            val everDisabled = viewModel.hasBatteryOptimizationEverBeenDisabled()
            
            if (!hasSkipped || everDisabled) {
                permissionState.setShowBatteryOptimizationDialog(true)
            }
        }
        viewModel.refreshFlashcardCount()
    }
    
    // Handle permission granted - start pending learning if permission was granted
    LaunchedEffect(permissionState.hasOverlayPermission) {
        if (permissionState.hasOverlayPermission) {
            viewModel.startPendingLearning()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(0.dp)
    ) {
        // Modern Header Section
        ModernHeaderSection(
            modifier = Modifier.fillMaxWidth()
        )
        
        // Content Section with responsive padding and scrolling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(contentPadding)
        ) {
            // Status Dashboard
            StatusDashboard(
                isServiceActive = uiState.isServiceActive,
                activeFlashcardCount = uiState.activeFlashcardCount,
                nextFlashcardCountdown = uiState.nextFlashcardCountdown,
                streak = uiState.statistics?.streakDays ?: 0,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Learning Controls with unified button (replaces old button + hint card)
            LearningControls(
                isServiceActive = uiState.isServiceActive,
                nextFlashcardCountdown = uiState.nextFlashcardCountdown,
                activeFlashcardCount = uiState.activeFlashcardCount,
                hasOverlayPermission = permissionState.hasOverlayPermission,
                onStartLearning = { showIntervalSelectionDialog = true },
                onStopLearning = { viewModel.toggleLearningService() },
                onRequestPermission = { permissionState.setShowOverlayPermissionDialog(true) },
                onNavigateToCards = onNavigateToSettings // Reuse existing navigation to cards
            )
            
            // Responsive Action Cards Grid
            if (isCompactScreen) {
                // 2x2 Grid for very small screens
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cardSpacing)
                    ) {
                        ResponsiveActionCard(
                            title = stringResource(R.string.home_cards_title),
                            subtitle = stringResource(R.string.home_cards_subtitle),
                            icon = "💼",
                            onClick = onNavigateToSettings,
                            isPrimary = true,
                            contentDescription = stringResource(R.string.home_cards_description),
                            cardHeight = cardHeight,
                            isCompact = isCompactScreen,
                            modifier = Modifier.weight(1f)
                        )
                        
                        ResponsiveActionCard(
                            title = stringResource(R.string.home_statistics_title),
                            subtitle = stringResource(R.string.home_statistics_subtitle),
                            icon = "📊",
                            onClick = onNavigateToStatistics,
                            isPrimary = false,
                            contentDescription = stringResource(R.string.home_statistics_description),
                            cardHeight = cardHeight,
                            isCompact = isCompactScreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Settings card on second row for compact screens
                    ResponsiveSettingsCard(
                        onClick = onNavigateToAppSettings,
                        cardHeight = cardHeight,
                        isCompact = isCompactScreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // 3x1 Grid for normal and larger screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    ResponsiveActionCard(
                        title = stringResource(R.string.home_cards_title),
                        subtitle = stringResource(R.string.home_cards_subtitle),
                        icon = "💼",
                        onClick = onNavigateToSettings,
                        isPrimary = true,
                        contentDescription = stringResource(R.string.home_cards_description),
                        cardHeight = cardHeight,
                        isCompact = isSmallScreen,
                        modifier = Modifier.weight(1f)
                    )
                    
                    ResponsiveActionCard(
                        title = stringResource(R.string.home_statistics_title),
                        subtitle = stringResource(R.string.home_statistics_subtitle),
                        icon = "📊",
                        onClick = onNavigateToStatistics,
                        isPrimary = false,
                        contentDescription = stringResource(R.string.home_statistics_description),
                        cardHeight = cardHeight,
                        isCompact = isSmallScreen,
                        modifier = Modifier.weight(1f)
                    )
                    
                    ResponsiveSettingsCard(
                        onClick = onNavigateToAppSettings,
                        cardHeight = cardHeight,
                        isCompact = isSmallScreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    
    // Unified Dialog System
    PermissionDialogs(
        permissionState = permissionState,
        onRequestOverlayPermission = onRequestOverlayPermission
    )
    
    // Backup dialogs removed - now handled in Welcome Screen
    // This follows DRY principle by eliminating code duplication
    
    if (showIntervalSelectionDialog) {
        IntervalSelectionDialog(
            availableIntervals = viewModel.getAvailableIntervals(),
            onConfirm = { interval ->
                showIntervalSelectionDialog = false
                // Check overlay permission before starting learning
                permissionState.refreshPermissionState()
                if (!permissionState.hasOverlayPermission) {
                    // Store interval for later use and show permission dialog
                    viewModel.setPendingInterval(interval)
                    permissionState.setShowOverlayPermissionDialog(true)
                } else {
                    // Permission granted - check if first-time demo should be shown
                    viewModel.updateInterval(interval) // Save interval first
                    
                    if (viewModel.shouldShowFirstDemo()) {
                        // Show demo flashcard instead of starting timer
                        OverlayService.startWithDemoFlashcard(context)
                    } else {
                        // Start regular learning session
                        viewModel.startLearningWithInterval(interval)
                    }
                }
            },
            onDismiss = { showIntervalSelectionDialog = false }
        )
    }
}
