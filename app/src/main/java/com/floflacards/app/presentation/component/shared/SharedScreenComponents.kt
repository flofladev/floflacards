package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

/**
 * Shared screen components to eliminate duplication between FlashcardManagementScreen and SettingsScreen.
 * Follows DRY principle and SOLID principles by creating reusable, single-responsibility components.
 */

/**
 * Modern top app bar with bulk action support.
 * Replaces both ModernTopAppBar and ModernSettingsTopAppBar to eliminate duplication.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernScreenTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    itemCount: Int,
    activeCount: Int,
    bulkActionState: BulkActionState,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit
) {
    TopAppBar(
        title = {
            BulkActionHeader(
                title = title,
                bulkActionState = bulkActionState
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.shared_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            CompactBulkActionToggle(
                bulkActionState = bulkActionState,
                onEnableAll = onEnableAll,
                onDisableAll = onDisableAll
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Generic stats card component.
 * Replaces both CategoryStatsCard and CategoriesStatsCard to eliminate duplication.
 */
@Composable
fun StatsCard(
    totalItems: Int,
    activeItems: Int,
    totalLabel: String = stringResource(R.string.shared_total),
    activeLabel: String = stringResource(R.string.shared_active), 
    inactiveLabel: String = stringResource(R.string.shared_inactive),
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = totalItems.toString(),
                label = totalLabel,
                icon = Icons.Outlined.Info,
                containerColor = containerColor
            )
            StatItem(
                value = activeItems.toString(),
                label = activeLabel,
                icon = Icons.Outlined.CheckCircle,
                containerColor = containerColor
            )
            StatItem(
                value = (totalItems - activeItems).toString(),
                label = inactiveLabel,
                icon = Icons.Outlined.Close,
                containerColor = containerColor
            )
        }
    }
}

/**
 * Generic stat item component.
 * Replaces both StatItem and CategoryStatItem to eliminate duplication.
 */
@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    val onContainerColor = if (containerColor == MaterialTheme.colorScheme.primaryContainer)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer
        
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onContainerColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = onContainerColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = onContainerColor.copy(alpha = 0.8f)
        )
    }
}

/**
 * Generic empty state card component.
 * Replaces both EmptyStateCard and EmptyCategoriesCard to eliminate duplication.
 */
@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText)
            }
        }
    }
}
