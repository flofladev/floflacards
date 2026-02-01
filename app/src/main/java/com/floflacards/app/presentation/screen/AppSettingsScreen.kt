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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.content.pm.PackageManager
import kotlin.random.Random
import com.floflacards.app.data.model.AppTheme
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.data.model.Language
import com.floflacards.app.presentation.component.BatteryOptimizationSettingItem
import com.floflacards.app.presentation.component.DonationDialog
import com.floflacards.app.presentation.component.getHeaderContainerColor
import com.floflacards.app.presentation.component.getHeaderContentColor
import com.floflacards.app.presentation.component.welcome.LanguageSelectionDialog
import com.floflacards.app.presentation.viewmodel.AppSettingsViewModel
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

/**
 * Professional app settings screen following Material Design 3 principles.
 * Centralized place for app preferences and support options.
 * Follows DRY, KISS, and SOLID principles with clean architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit
) {
    var showDonationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Get ViewModel with SettingsRepository
    val viewModel: AppSettingsViewModel = hiltViewModel()
    val currentTheme by viewModel.appTheme.collectAsState()
    val currentFlashcardTheme by viewModel.flashcardTheme.collectAsState()
    val currentLanguage: Language by viewModel.appLocale.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_go_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = getHeaderContainerColor(),
                    titleContentColor = getHeaderContentColor(),
                    navigationIconContentColor = getHeaderContentColor()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language section - VERY TOP as requested
            item {
                AppSettingsSection(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_subtitle)
                ) {
                    LanguageSettingItem(
                        currentLanguage = currentLanguage,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }
            
            // Appearance section - SECOND section as requested
            item {
                AppSettingsSection(
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = stringResource(R.string.settings_appearance_subtitle)
                ) {
                    // App Theme subsection
                    Text(
                        text = stringResource(R.string.settings_appearance_app_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    ThemeSelectionItem(
                        currentTheme = currentTheme,
                        onThemeSelected = { theme -> viewModel.setAppTheme(theme) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Flashcard Theme subsection
                    Text(
                        text = stringResource(R.string.settings_appearance_flashcard_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.settings_appearance_flashcard_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    FlashcardThemeSelectionItem(
                        currentTheme = currentFlashcardTheme,
                        onThemeSelected = { theme -> viewModel.setFlashcardTheme(theme) }
                    )
                }
            }
            
            // Permissions section - SECOND section as requested
            item {
                AppSettingsSection(
                    title = stringResource(R.string.settings_permissions_title),
                    subtitle = stringResource(R.string.settings_permissions_subtitle)
                ) {
                    BatteryOptimizationSettingItem(
                        viewModel = viewModel
                    )
                }
            }
            
            item {
                AppSettingsSection(
                    title = stringResource(R.string.settings_support_title),
                    subtitle = stringResource(R.string.settings_support_subtitle)
                ) {
                    SupportSettingItem(
                        title = stringResource(R.string.settings_support_development_title),
                        subtitle = stringResource(R.string.settings_support_development_subtitle),
                        icon = Icons.Default.Favorite,
                        onClick = { showDonationDialog = true }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ContactDeveloperItem(
                        title = stringResource(R.string.settings_support_contact_title),
                        subtitle = stringResource(R.string.settings_support_contact_subtitle),
                        email = "flofladev@gmail.com"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val context = LocalContext.current
                    RateAppItem(
                        title = stringResource(R.string.settings_support_rate_title),
                        subtitle = stringResource(R.string.settings_support_rate_subtitle),
                        onClick = {
                            val packageName = context.packageName
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=$packageName")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // If Play Store app is not available, open in browser
                                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                                }
                                try {
                                    context.startActivity(webIntent)
                                } catch (e: Exception) {
                                    // Handle case where no browser is available
                                }
                            }
                        }
                    )
                }
            }
            
            item {
                AppSettingsSection(
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_subtitle)
                ) {
                    AboutSettingItem(
                        title = stringResource(R.string.settings_about_app_title),
                        subtitle = stringResource(R.string.settings_about_app_subtitle),
                        icon = Icons.Default.Info
                    )
                    
                    AboutSettingItem(
                        title = stringResource(R.string.settings_about_creator_title),
                        subtitle = stringResource(R.string.settings_about_creator_subtitle),
                        icon = Icons.Default.Person
                    )
                    
                    AboutSettingItem(
                        title = stringResource(R.string.settings_about_version_title),
                        subtitle = getAppVersion(LocalContext.current),
                        icon = Icons.Default.Build
                    )
                }
            }
        }
    }
    
    // Donation Dialog
    if (showDonationDialog) {
        DonationDialog(
            onDismiss = { showDonationDialog = false }
        )
    }
    
    // Language Selection Dialog  
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { selectedLanguage ->
                // Apply the language change through SettingsRepository
                viewModel.setAppLocale(selectedLanguage)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * Reusable settings section component.
 * Follows app's card-based design patterns.
 */
@Composable
private fun AppSettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            content()
        }
    }
}

/**
 * Interactive setting item for support actions.
 * Matches app's modern button design.
 */
@Composable
private fun SupportSettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Static information setting item.
 * For displaying app information without interaction.
 */
@Composable
private fun AboutSettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Interactive contact developer item.
 * Opens email client when clicked.
 * Moved to Support section for better organization and user experience.
 */
@Composable
private fun ContactDeveloperItem(
    title: String,
    subtitle: String,
    email: String
) {
    val context = LocalContext.current
    
    Card(
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Floating Learning - Feedback")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Handle case where no email client is available
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
                
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Theme selection component for the Appearance section.
 * Provides radio button selection for Light, Dark, and System themes.
 * Follows Material Design 3 principles and app's design patterns.
 */
@Composable
fun ThemeSelectionItem(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        AppTheme.values().forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (theme == currentTheme),
                        onClick = { onThemeSelected(theme) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (theme == currentTheme),
                    onClick = null // handled by selectable modifier
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = when (theme) {
                            AppTheme.LIGHT -> stringResource(R.string.theme_light_name)
                            AppTheme.DARK -> stringResource(R.string.theme_dark_name)
                            AppTheme.SYSTEM -> stringResource(R.string.theme_system_name)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = when (theme) {
                            AppTheme.LIGHT -> stringResource(R.string.theme_light_description)
                            AppTheme.DARK -> stringResource(R.string.theme_dark_description)
                            AppTheme.SYSTEM -> stringResource(R.string.theme_system_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Flashcard theme selection component for the Appearance section.
 * Provides radio button selection for Default, Light, and Dark flashcard themes.
 * Follows Material Design 3 principles and app's design patterns.
 * CRITICAL: Flashcard theme is independent from both app and device theme.
 */
@Composable
fun FlashcardThemeSelectionItem(
    currentTheme: FlashcardTheme,
    onThemeSelected: (FlashcardTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        FlashcardTheme.values().forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (theme == currentTheme),
                        onClick = { onThemeSelected(theme) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (theme == currentTheme),
                    onClick = null // handled by selectable modifier
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = when (theme) {
                            FlashcardTheme.DEFAULT -> stringResource(R.string.flashcard_theme_default_name)
                            FlashcardTheme.LIGHT -> stringResource(R.string.flashcard_theme_light_name)
                            FlashcardTheme.DARK -> stringResource(R.string.flashcard_theme_dark_name)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = when (theme) {
                            FlashcardTheme.DEFAULT -> stringResource(R.string.flashcard_theme_default_description)
                            FlashcardTheme.LIGHT -> stringResource(R.string.flashcard_theme_light_description)
                            FlashcardTheme.DARK -> stringResource(R.string.flashcard_theme_dark_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Rate app item that redirects to a random app on Play Store.
 * Temporary solution until our app is published on Play Store.
 */
@Composable
fun RateAppItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Language setting item component for the Language section.
 * Shows current language with flag and name, clickable to open language selection dialog.
 * Matches the app's modern setting item design.
 */
@Composable
private fun LanguageSettingItem(
    currentLanguage: Language,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag emoji
            Text(
                text = currentLanguage.flagEmoji,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_language_current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                
                Text(
                    text = currentLanguage.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Get app version from PackageManager.
 * Follows SOLID principles - Single Responsibility for version retrieval.
 * Uses Android best practices for dynamic version access.
 */
private fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}
