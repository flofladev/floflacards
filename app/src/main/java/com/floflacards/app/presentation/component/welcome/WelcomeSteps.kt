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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floflacards.app.R
import com.floflacards.app.data.backup.BackupInfo
import com.floflacards.app.data.model.Language
import com.floflacards.app.presentation.component.welcome.LanguageSelectionDialog
import com.floflacards.app.presentation.component.welcome.WelcomeLanguageSelectionDialog

/**
 * Welcome onboarding step components extracted from WelcomeScreen.kt.
 * Follows SOLID principles - each step has single responsibility.
 * Follows DRY principle - reuses WelcomeStepCard component.
 * Follows KISS principle - simple, focused step implementations.
 */

/**
 * Introduction step with streamlined question-hook approach.
 * Focuses on core value proposition with minimal text for better mobile UX.
 * Includes full-width language selection button for better mobile accessibility.
 */
@Composable
fun IntroductionStep(
    onNext: () -> Unit,
    onLanguageChanged: ((Language) -> Unit)? = null
) {
    // Inject AppSettingsViewModel to get current language preference
    val viewModel: com.floflacards.app.presentation.viewmodel.AppSettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    // Get current language from SettingsRepository, not hardcoded English
    val currentLanguage: Language by viewModel.appLocale.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    WelcomeStepCard(
        title = stringResource(R.string.welcome_intro_title),
        content = {
            Column {
                // Core value proposition - clear and concise
                Text(
                    text = stringResource(R.string.welcome_intro_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Single compelling benefit with prominent styling
                Text(
                    text = stringResource(R.string.welcome_intro_benefit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        buttonText = stringResource(R.string.welcome_intro_button),
        onButtonClick = { showLanguageDialog = true }, // Changed: Open language dialog instead of proceeding
        isButtonEnabled = true,
        secondaryButtonText = "🌐 ${currentLanguage.flagEmoji} ${currentLanguage.displayName}",
        onSecondaryButtonClick = { showLanguageDialog = true }
    )
    
    // Language selection dialog
    if (showLanguageDialog) {
        WelcomeLanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { selectedLanguage ->
                // Apply the language change through ViewModel
                viewModel.setAppLocale(selectedLanguage)
                onLanguageChanged?.invoke(selectedLanguage)
                
                // Auto-progress to next step after language selection
                showLanguageDialog = false
                onNext()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * Privacy step with visual flow approach.
 * Emphasizes trust through simple, clear messaging and visual representation.
 */
@Composable
fun PrivacyOfflineStep(onNext: () -> Unit) {
    WelcomeStepCard(
        title = stringResource(R.string.welcome_privacy_title),
        content = {
            Column {
                // Core privacy promise - clear and concise
                Text(
                    text = stringResource(R.string.welcome_privacy_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Visual flow representation
                Text(
                    text = stringResource(R.string.welcome_privacy_flow),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        buttonText = stringResource(R.string.welcome_privacy_button),
        onButtonClick = onNext,
        isButtonEnabled = true
    )
}

/**
 * Backup folder selection step using Storage Access Framework (SAF).
 */
@Composable
fun BackupFolderStep(
    hasFolderConfigured: Boolean,
    onRequestFolderSelection: () -> Unit,
    onNext: () -> Unit
) {
    WelcomeStepCard(
        title = stringResource(R.string.welcome_backup_title),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.welcome_backup_description),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = stringResource(R.string.welcome_backup_benefits_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val benefits = listOf(
                    stringResource(R.string.welcome_backup_benefit_1),
                    stringResource(R.string.welcome_backup_benefit_2),
                    stringResource(R.string.welcome_backup_benefit_3),
                    stringResource(R.string.welcome_backup_benefit_4)
                )
                
                benefits.forEach { benefit ->
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PermissionStatusIndicator(
                    isGranted = hasFolderConfigured,
                    grantedText = stringResource(R.string.welcome_backup_granted),
                    deniedText = stringResource(R.string.welcome_backup_required)
                )
            }
        },
        buttonText = if (hasFolderConfigured) stringResource(R.string.welcome_backup_button_continue) else stringResource(R.string.welcome_backup_button_select),
        onButtonClick = if (hasFolderConfigured) onNext else onRequestFolderSelection,
        isButtonEnabled = true
    )
}

/**
 * Overlay permission request step.
 */
@Composable
fun OverlayPermissionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    WelcomeStepCard(
        title = stringResource(R.string.welcome_overlay_title),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.welcome_overlay_description),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                PermissionStatusIndicator(
                    isGranted = hasPermission,
                    grantedText = stringResource(R.string.welcome_overlay_granted),
                    deniedText = stringResource(R.string.welcome_overlay_required)
                )
            }
        },
        buttonText = if (hasPermission) stringResource(R.string.welcome_overlay_button_continue) else stringResource(R.string.welcome_overlay_button_grant),
        onButtonClick = if (hasPermission) onNext else onRequestPermission,
        isButtonEnabled = true
    )
}

/**
 * Battery optimization disable step with skip option.
 * Follows SOLID principles by separating concerns and providing clear user choices.
 */
@Composable
fun BatteryOptimizationStep(
    isOptimizationDisabled: Boolean,
    onRequestDisable: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    if (isOptimizationDisabled) {
        // Show simple continue button when optimization is already disabled
        WelcomeStepCard(
            title = stringResource(R.string.welcome_battery_title),
            content = {
                Column {
                    Text(
                        text = stringResource(R.string.welcome_battery_description),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    PermissionStatusIndicator(
                        isGranted = true,
                        grantedText = stringResource(R.string.welcome_battery_granted),
                        deniedText = stringResource(R.string.welcome_battery_enabled)
                    )
                }
            },
            buttonText = stringResource(R.string.welcome_battery_button_continue),
            onButtonClick = onNext,
            isButtonEnabled = true
        )
    } else {
        // Show both disable and skip options when optimization is enabled
        WelcomeStepCard(
            title = stringResource(R.string.welcome_battery_title),
            content = {
                Column {
                    Text(
                        text = stringResource(R.string.welcome_battery_description),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    PermissionStatusIndicator(
                        isGranted = false,
                        grantedText = stringResource(R.string.welcome_battery_granted),
                        deniedText = stringResource(R.string.welcome_battery_enabled)
                    )
                }
            },
            buttonText = stringResource(R.string.welcome_battery_button_disable),
            onButtonClick = onRequestDisable,
            isButtonEnabled = true,
            secondaryButtonText = stringResource(R.string.welcome_battery_button_skip),
            onSecondaryButtonClick = onSkip
        )
    }
}

/**
 * Backup check and restore step.
 */
@Composable
fun BackupCheckStep(
    hasBackup: Boolean,
    backupInfo: BackupInfo,
    onRestore: () -> Unit,
    onSkip: () -> Unit
) {
    WelcomeStepCard(
        title = if (hasBackup) stringResource(R.string.welcome_backup_check_found_title) else stringResource(R.string.welcome_backup_check_fresh_title),
        content = {
            Column {
                if (hasBackup) {
                    Text(
                        text = stringResource(R.string.welcome_backup_check_found_description),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.welcome_backup_check_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = stringResource(
                            R.string.welcome_backup_check_created,
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(backupInfo.createdAt))
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = stringResource(
                            R.string.welcome_backup_check_categories,
                            backupInfo.categoryCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = stringResource(
                            R.string.welcome_backup_check_flashcards,
                            backupInfo.flashcardCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.welcome_backup_check_question),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = stringResource(R.string.welcome_backup_check_fresh_description),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        },
        buttonText = if (hasBackup) stringResource(R.string.welcome_backup_check_button_restore) else stringResource(R.string.welcome_backup_check_button_fresh),
        onButtonClick = if (hasBackup) onRestore else onSkip,
        isButtonEnabled = true,
        secondaryButtonText = if (hasBackup) stringResource(R.string.welcome_backup_check_button_fresh) else null,
        onSecondaryButtonClick = if (hasBackup) onSkip else null
    )
}

/**
 * Welcome completion step.
 */
@Composable
fun CompletedStep(onEnterApp: () -> Unit) {
    WelcomeStepCard(
        title = stringResource(R.string.welcome_completed_title),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.welcome_completed_description),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = stringResource(R.string.welcome_completed_ready_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val readyFeatures = listOf(
                    stringResource(R.string.welcome_completed_feature_1),
                    stringResource(R.string.welcome_completed_feature_2),
                    stringResource(R.string.welcome_completed_feature_3),
                    stringResource(R.string.welcome_completed_feature_4)
                )
                
                readyFeatures.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.welcome_completed_happy_learning),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        },
        buttonText = stringResource(R.string.welcome_completed_button),
        onButtonClick = onEnterApp,
        isButtonEnabled = true
    )
}
