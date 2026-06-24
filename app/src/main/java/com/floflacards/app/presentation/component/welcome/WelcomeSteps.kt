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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.data.backup.BackupInfo
import com.floflacards.app.data.model.Language
import com.floflacards.app.presentation.component.welcome.WelcomeLanguageSelectionDialog
import com.floflacards.app.presentation.viewmodel.AppSettingsViewModel

/**
 * Redesigned welcome onboarding steps, grouped by intent into four screens:
 * Welcome (intro + privacy + language), Permissions (overlay + battery),
 * Backup (folder + inline restore), and Completed.
 */

/* ----------------------------------------------------------------------- */
/* Step 1 · Welcome + language                                             */
/* ----------------------------------------------------------------------- */

@Composable
fun WelcomeIntroStep(
    onGetStarted: () -> Unit,
    onLanguageChanged: ((Language) -> Unit)? = null
) {
    val viewModel: AppSettingsViewModel = hiltViewModel()
    val currentLanguage: Language by viewModel.appLocale.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    WelcomeStepCard(
        title = stringResource(R.string.welcome_intro_title),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.welcome_intro_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Trust line (replaces the old emoji "Local -> Secure -> Private").
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.welcome_privacy_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LanguageSelectorRow(currentLanguage) { showLanguageDialog = true }
            }
        },
        buttonText = stringResource(R.string.welcome_intro_button),
        onButtonClick = onGetStarted,
        isButtonEnabled = true
    )

    if (showLanguageDialog) {
        WelcomeLanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { selected ->
                viewModel.setAppLocale(selected)
                onLanguageChanged?.invoke(selected) // host recreates the activity to apply locale
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun LanguageSelectorRow(language: Language, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = "${language.flagEmoji}  ${language.displayName}",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
    }
}

/* ----------------------------------------------------------------------- */
/* Step 2 · Permissions (overlay + battery)                                */
/* ----------------------------------------------------------------------- */

@Composable
fun PermissionsStep(
    hasOverlayPermission: Boolean,
    isBatteryOptimizationDisabled: Boolean,
    onGrantOverlay: () -> Unit,
    onAllowBattery: () -> Unit,
    onContinue: () -> Unit
) {
    WelcomeStepCard(
        title = stringResource(R.string.welcome_permissions_title),
        content = {
            Column {
                PermissionRow(
                    icon = Icons.Outlined.Layers,
                    label = stringResource(R.string.welcome_overlay_label),
                    granted = hasOverlayPermission,
                    actionText = stringResource(R.string.welcome_overlay_button_grant),
                    onAction = onGrantOverlay
                )
                Spacer(Modifier.height(8.dp))
                PermissionRow(
                    icon = Icons.Outlined.BatteryStd,
                    label = stringResource(R.string.welcome_battery_label),
                    granted = isBatteryOptimizationDisabled,
                    actionText = stringResource(R.string.welcome_battery_button_disable),
                    onAction = onAllowBattery
                )
            }
        },
        buttonText = stringResource(R.string.welcome_overlay_button_continue),
        onButtonClick = onContinue,
        isButtonEnabled = hasOverlayPermission // overlay is required; battery is optional
    )
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.welcome_backup_granted),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            FilledTonalButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Step 3 · Backup folder + inline restore                                 */
/* ----------------------------------------------------------------------- */

@Composable
fun BackupStep(
    hasFolderConfigured: Boolean,
    hasBackup: Boolean,
    backupInfo: BackupInfo,
    isRestoring: Boolean,
    onChooseFolder: () -> Unit,
    onRestore: () -> Unit,
    onStartFresh: () -> Unit
) {
    // Primary / secondary buttons depend on the state of the folder + backup detection.
    val primaryText: String
    val primaryAction: () -> Unit
    var secondaryText: String? = null
    var secondaryAction: (() -> Unit)? = null

    when {
        !hasFolderConfigured -> {
            primaryText = stringResource(R.string.welcome_backup_button_select)
            primaryAction = onChooseFolder
            secondaryText = stringResource(R.string.welcome_backup_skip)
            secondaryAction = onStartFresh
        }
        hasBackup -> {
            primaryText = stringResource(R.string.welcome_backup_check_button_restore)
            primaryAction = onRestore
            secondaryText = stringResource(R.string.welcome_backup_check_button_fresh)
            secondaryAction = onStartFresh
        }
        else -> {
            primaryText = stringResource(R.string.welcome_backup_button_continue)
            primaryAction = onStartFresh
        }
    }

    WelcomeStepCard(
        title = stringResource(R.string.welcome_backup_title),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    isRestoring -> {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                    !hasFolderConfigured -> {
                        Text(
                            text = stringResource(R.string.welcome_backup_description),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    hasBackup -> {
                        StatusLine(stringResource(R.string.welcome_backup_check_found_description))
                        Spacer(Modifier.height(12.dp))
                        BackupDetail(
                            stringResource(
                                R.string.welcome_backup_check_created,
                                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                    .format(java.util.Date(backupInfo.createdAt))
                            )
                        )
                        BackupDetail(stringResource(R.string.welcome_backup_check_categories, backupInfo.categoryCount))
                        BackupDetail(stringResource(R.string.welcome_backup_check_flashcards, backupInfo.flashcardCount))
                    }
                    else -> {
                        StatusLine(stringResource(R.string.welcome_backup_granted))
                    }
                }
            }
        },
        buttonText = primaryText,
        onButtonClick = primaryAction,
        isButtonEnabled = !isRestoring,
        secondaryButtonText = if (isRestoring) null else secondaryText,
        onSecondaryButtonClick = if (isRestoring) null else secondaryAction
    )
}

@Composable
private fun StatusLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BackupDetail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/* ----------------------------------------------------------------------- */
/* Step 4 · Completed                                                      */
/* ----------------------------------------------------------------------- */

@Composable
fun CompletedStep(onEnterApp: () -> Unit) {
    WelcomeStepCard(
        title = stringResource(R.string.welcome_completed_title),
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.welcome_completed_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        buttonText = stringResource(R.string.welcome_completed_button),
        onButtonClick = onEnterApp,
        isButtonEnabled = true
    )
}
