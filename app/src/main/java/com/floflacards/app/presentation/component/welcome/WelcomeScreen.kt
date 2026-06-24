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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.floflacards.app.presentation.viewmodel.WelcomeStep
import com.floflacards.app.presentation.viewmodel.WelcomeViewModel

/**
 * Redesigned welcome screen: four screens grouped by intent, a minimalist segmented
 * progress bar, and theme-aware styling (no hardcoded colors or emojis).
 */
@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onRequestOverlayPermission: () -> Unit,
    onRequestBackupFolder: () -> Unit,
    onWelcomeCompleted: () -> Unit,
    onLanguageChanged: ((com.floflacards.app.data.model.Language) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WelcomeProgressIndicator(
            currentStep = uiState.currentStep,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Center the card in the remaining space (scrolls if the content is tall),
        // so it sits in the middle of the screen instead of floating at the top.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        when (uiState.currentStep) {
            WelcomeStep.WELCOME -> WelcomeIntroStep(
                onGetStarted = { viewModel.nextStep() },
                onLanguageChanged = onLanguageChanged
            )
            WelcomeStep.PERMISSIONS -> PermissionsStep(
                hasOverlayPermission = uiState.hasOverlayPermission,
                isBatteryOptimizationDisabled = uiState.isBatteryOptimizationDisabled,
                onGrantOverlay = onRequestOverlayPermission,
                onAllowBattery = { viewModel.requestBatteryOptimizationDisable() },
                onContinue = { viewModel.proceedFromPermissions() }
            )
            WelcomeStep.BACKUP -> BackupStep(
                hasFolderConfigured = uiState.hasBackupFolderConfigured,
                hasBackup = uiState.hasBackupAvailable,
                backupInfo = uiState.backupInfo,
                isRestoring = uiState.isRestoring,
                onChooseFolder = onRequestBackupFolder,
                onRestore = { viewModel.restoreBackup() },
                onStartFresh = { viewModel.startFresh() }
            )
            WelcomeStep.COMPLETED -> CompletedStep(
                onEnterApp = onWelcomeCompleted
            )
        }
        }
    }
}

@Composable
private fun WelcomeProgressIndicator(
    currentStep: WelcomeStep,
    modifier: Modifier = Modifier
) {
    // Three setup screens; COMPLETED shows everything filled.
    val totalSegments = 3
    val currentIndex = when (currentStep) {
        WelcomeStep.WELCOME -> 0
        WelcomeStep.PERMISSIONS -> 1
        WelcomeStep.BACKUP -> 2
        WelcomeStep.COMPLETED -> 2
    }

    Row(modifier = modifier.fillMaxWidth()) {
        for (i in 0 until totalSegments) {
            val filled = i <= currentIndex
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
            if (i < totalSegments - 1) Spacer(Modifier.width(8.dp))
        }
    }
}
