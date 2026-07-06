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

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.presentation.viewmodel.BackupViewModel
import com.floflacards.app.presentation.viewmodel.DataActionResult
import com.floflacards.app.util.DateUtils

/**
 * Content of the Settings -> Data section: backup folder, automatic backup
 * toggle, manual backup, and restore. Rendered inside an AppSettingsSection
 * card by AppSettingsScreen.
 */
@Composable
fun DataSettingsContent(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showRestoreConfirm by remember { mutableStateOf(false) }

    // Mirrors the welcome flow's folder selection exactly (same intent flags,
    // same persistable permission), so both entry points behave identically.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { treeUri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    viewModel.onFolderPicked(treeUri.toString())
                } catch (e: SecurityException) {
                    // Provider refused a persistable grant; keep the previous folder.
                }
            }
        }
    }

    val busy = state.isBackingUp || state.isRestoring

    Column {
        DataActionRow(
            icon = Icons.Outlined.Folder,
            title = stringResource(R.string.data_backup_folder_title),
            subtitle = state.folderName
                ?.let { stringResource(R.string.data_backup_folder_change, it) }
                ?: stringResource(R.string.data_backup_folder_not_set),
            enabled = !busy,
            onClick = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                folderPicker.launch(intent)
            }
        )

        DataSwitchRow(
            title = stringResource(R.string.data_auto_backup_title),
            subtitle = if (state.hasFolderConfigured) {
                stringResource(R.string.data_auto_backup_subtitle)
            } else {
                stringResource(R.string.data_needs_folder)
            },
            checked = state.autoBackupEnabled,
            enabled = state.hasFolderConfigured && !busy,
            onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
        )

        DataActionRow(
            icon = Icons.Outlined.Save,
            title = stringResource(R.string.data_backup_now_title),
            subtitle = when {
                !state.hasFolderConfigured -> stringResource(R.string.data_needs_folder)
                state.backupInfo.exists -> stringResource(
                    R.string.data_last_backup,
                    DateUtils.formatDateTime(state.backupInfo.createdAt)
                )
                else -> stringResource(R.string.data_no_backup_yet)
            },
            enabled = state.hasFolderConfigured && !busy,
            showProgress = state.isBackingUp,
            onClick = { viewModel.backupNow() }
        )

        DataActionRow(
            icon = Icons.Outlined.SettingsBackupRestore,
            title = stringResource(R.string.data_restore_title),
            subtitle = when {
                !state.hasFolderConfigured -> stringResource(R.string.data_needs_folder)
                state.backupInfo.exists -> stringResource(
                    R.string.data_restore_details,
                    DateUtils.formatDateTime(state.backupInfo.createdAt),
                    state.backupInfo.categoryCount,
                    state.backupInfo.flashcardCount
                )
                else -> stringResource(R.string.data_no_backup_yet)
            },
            enabled = state.hasFolderConfigured && state.backupInfo.exists && !busy,
            showProgress = state.isRestoring,
            onClick = { showRestoreConfirm = true }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(stringResource(R.string.data_restore_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.data_restore_confirm_message,
                        DateUtils.formatDateTime(state.backupInfo.createdAt)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        viewModel.restoreBackup()
                    }
                ) {
                    Text(stringResource(R.string.welcome_backup_check_button_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    state.lastActionResult?.let { result ->
        val (title, message) = when (result) {
            DataActionResult.BACKUP_SUCCESS ->
                stringResource(R.string.data_backup_success_title) to
                    stringResource(R.string.data_backup_success_message)
            DataActionResult.BACKUP_FAILED ->
                stringResource(R.string.data_action_failed_title) to
                    stringResource(R.string.data_backup_failed_message)
            DataActionResult.BACKUP_BLOCKED_EMPTY ->
                stringResource(R.string.data_action_failed_title) to
                    stringResource(R.string.data_backup_blocked_empty_message)
            DataActionResult.RESTORE_SUCCESS ->
                stringResource(R.string.data_restore_success_title) to
                    stringResource(
                        R.string.data_restore_success_message,
                        state.restoredCategories,
                        state.restoredFlashcards
                    )
            DataActionResult.RESTORE_FAILED ->
                stringResource(R.string.data_action_failed_title) to
                    stringResource(R.string.data_restore_failed_message)
        }
        AlertDialog(
            onDismissRequest = { viewModel.clearActionResult() },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearActionResult() }) {
                    Text(stringResource(R.string.data_dialog_ok))
                }
            }
        )
    }
}

/**
 * Clickable row with leading icon, title/subtitle, and a trailing chevron
 * (or progress spinner while its action runs). Disabled rows are dimmed.
 */
@Composable
private fun DataActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    showProgress: Boolean = false
) {
    val contentAlpha = if (enabled || showProgress) 1f else 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha * 0.6f)
            )
        }
    }
}

/**
 * Row with a trailing switch, matching the Behavior section's switch row.
 * Dimmed and inert while no backup folder is configured.
 */
@Composable
private fun DataSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = null, // handled by toggleable modifier
            enabled = enabled
        )
    }
}
