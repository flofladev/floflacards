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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

/**
 * Data class representing the current bulk action state
 */
data class BulkActionState(
    val totalCount: Int,
    val enabledCount: Int,
    val isAllEnabled: Boolean = enabledCount == totalCount && totalCount > 0,
    val isAllDisabled: Boolean = enabledCount == 0 && totalCount > 0,
    val hasItems: Boolean = totalCount > 0
)

/**
 * Reusable bulk action button component following Material Design 3 principles
 * Provides consistent "Select All" / "Deselect All" functionality
 */
@Composable
fun BulkActionButton(
    bulkActionState: BulkActionState,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    modifier: Modifier = Modifier,
    enableAllText: String = stringResource(R.string.shared_enable_all),
    disableAllText: String = stringResource(R.string.shared_disable_all)
) {
    AnimatedVisibility(
        visible = bulkActionState.hasItems,
        enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)),
        modifier = modifier
    ) {
        val isAllEnabled = bulkActionState.isAllEnabled
        val buttonText = if (isAllEnabled) disableAllText else enableAllText
        val buttonIcon = if (isAllEnabled) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
        val buttonAction = if (isAllEnabled) onDisableAll else onEnableAll
        
        Button(
            onClick = buttonAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAllEnabled) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isAllEnabled)
                    MaterialTheme.colorScheme.onError
                else
                    MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Header component showing current selection state with animation
 * Displays useful information about enabled/total counts
 */
@Composable
fun BulkActionHeader(
    title: String,
    bulkActionState: BulkActionState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        AnimatedVisibility(
            visible = bulkActionState.hasItems,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Text(
                text = stringResource(R.string.bulk_action_active_count, bulkActionState.enabledCount, bulkActionState.totalCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Confirmation dialog for bulk operations
 * Provides clear feedback about the action being performed
 */
@Composable
fun BulkActionConfirmationDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructiveAction: Boolean = false
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructiveAction)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        contentColor = if (isDestructiveAction)
                            MaterialTheme.colorScheme.onError
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.shared_cancel))
                }
            }
        )
    }
}

/**
 * Compact bulk action toggle for use in top app bars or constrained spaces
 */
@Composable
fun CompactBulkActionToggle(
    bulkActionState: BulkActionState,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = bulkActionState.hasItems,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        val isAllEnabled = bulkActionState.isAllEnabled
        val icon = if (isAllEnabled) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
        val action = if (isAllEnabled) onDisableAll else onEnableAll
        val contentDescription = if (isAllEnabled) stringResource(R.string.shared_disable_all_items) else stringResource(R.string.shared_enable_all_items)
        
        IconButton(
            onClick = action,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isAllEnabled)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Extension function to calculate bulk action state from lists
 */
fun <T> List<T>.toBulkActionState(isEnabledPredicate: (T) -> Boolean): BulkActionState {
    val totalCount = this.size
    val enabledCount = this.count(isEnabledPredicate)
    return BulkActionState(
        totalCount = totalCount,
        enabledCount = enabledCount
    )
}
