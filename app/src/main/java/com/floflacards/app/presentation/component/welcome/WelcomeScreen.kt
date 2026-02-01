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

package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floflacards.app.R
import com.floflacards.app.presentation.viewmodel.WelcomeViewModel
import com.floflacards.app.presentation.viewmodel.WelcomeStep

/**
 * Welcome Screen component following SRP.
 * Handles mandatory onboarding flow with permission checks.
 * Follows KISS principle with clear, step-by-step progression.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onRequestOverlayPermission: () -> Unit,
    onRequestBackupFolder: () -> Unit,
    onWelcomeCompleted: () -> Unit,
    onLanguageChanged: ((com.floflacards.app.data.model.Language) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Move to completion step when all permissions are granted
    LaunchedEffect(uiState) {
        if (viewModel.canCompleteWelcome() && uiState.currentStep != WelcomeStep.COMPLETED) {
            viewModel.nextStep() // Move to COMPLETED step instead of auto-completing
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        WelcomeProgressIndicator(
            currentStep = uiState.currentStep,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Step content
        when (uiState.currentStep) {
            WelcomeStep.INTRODUCTION -> IntroductionStep(
                onNext = { viewModel.nextStep() },
                onLanguageChanged = onLanguageChanged
            )
            WelcomeStep.PRIVACY_OFFLINE -> PrivacyOfflineStep(
                onNext = { viewModel.nextStep() }
            )
            WelcomeStep.BACKUP_FOLDER -> BackupFolderStep(
                hasFolderConfigured = uiState.hasBackupFolderConfigured,
                onRequestFolderSelection = onRequestBackupFolder,
                onNext = { viewModel.nextStep() }
            )
            WelcomeStep.OVERLAY_PERMISSION -> OverlayPermissionStep(
                hasPermission = uiState.hasOverlayPermission,
                onRequestPermission = onRequestOverlayPermission,
                onNext = { viewModel.nextStep() }
            )
            WelcomeStep.BATTERY_OPTIMIZATION -> BatteryOptimizationStep(
                isOptimizationDisabled = uiState.isBatteryOptimizationDisabled,
                onRequestDisable = { viewModel.requestBatteryOptimizationDisable() },
                onSkip = { viewModel.skipBatteryOptimization() },
                onNext = { viewModel.nextStep() }
            )
            WelcomeStep.BACKUP_CHECK -> BackupCheckStep(
                hasBackup = uiState.hasBackupAvailable,
                backupInfo = uiState.backupInfo,
                onRestore = { viewModel.restoreBackup() },
                onSkip = { viewModel.nextStep() }
            )
            WelcomeStep.COMPLETED -> CompletedStep(
                onEnterApp = onWelcomeCompleted
            )
        }
    }
}

@Composable
private fun WelcomeProgressIndicator(
    currentStep: WelcomeStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        stringResource(R.string.welcome_progress_introduction),
        stringResource(R.string.welcome_progress_privacy),
        stringResource(R.string.welcome_progress_backup_folder),
        stringResource(R.string.welcome_progress_overlay),
        stringResource(R.string.welcome_progress_battery),
        stringResource(R.string.welcome_progress_backup_check)
    )
    
    val currentStepIndex = when (currentStep) {
        WelcomeStep.INTRODUCTION -> 0
        WelcomeStep.PRIVACY_OFFLINE -> 1
        WelcomeStep.BACKUP_FOLDER -> 2
        WelcomeStep.OVERLAY_PERMISSION -> 3
        WelcomeStep.BATTERY_OPTIMIZATION -> 4
        WelcomeStep.BACKUP_CHECK -> 5
        WelcomeStep.COMPLETED -> 5
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.welcome_progress_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LinearProgressIndicator(
            progress = { (currentStepIndex + 1) / steps.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFFE0E0E0)
        )
        
        Text(
            text = stringResource(
                R.string.welcome_progress_step,
                currentStepIndex + 1,
                steps.size,
                steps[currentStepIndex]
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}









