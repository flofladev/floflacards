package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floflacards.app.presentation.viewmodel.AppSettingsViewModel

/**
 * Battery optimization setting item for the Permissions section.
 * Allows users to manage battery optimization preferences and re-enable dialogs.
 * Follows app's design patterns and Material Design 3 principles.
 * Follows SOLID principles with single responsibility for battery optimization settings.
 */
@Composable
fun BatteryOptimizationSettingItem(
    viewModel: AppSettingsViewModel
) {
    val context = LocalContext.current
    var isOptimizationDisabled by remember { mutableStateOf(false) }
    var isSkipped by remember { mutableStateOf(false) }
    
    // Check current states
    LaunchedEffect(Unit) {
        isOptimizationDisabled = viewModel.isBatteryOptimizationDisabled()
        isSkipped = viewModel.isBatteryOptimizationSkipped()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Battery Optimization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    val statusText = when {
                        isOptimizationDisabled -> "Disabled - App can run in background"
                        isSkipped -> "If any performance issues occur, try disabling battery optimization"
                        else -> "Enabled - May affect app performance"
                    }
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action button - only show when optimization is enabled
            if (!isOptimizationDisabled) {
                Button(
                    onClick = {
                        viewModel.requestBatteryOptimizationDisable()
                        // Refresh states after request
                        isOptimizationDisabled = viewModel.isBatteryOptimizationDisabled()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable Optimization")
                }
            }
        }
    }
}
