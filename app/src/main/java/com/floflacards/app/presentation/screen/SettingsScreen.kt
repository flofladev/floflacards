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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.presentation.viewmodel.CategoryViewModel
import com.floflacards.app.presentation.component.UnifiedDialog
import com.floflacards.app.presentation.component.DeleteCategoryConfirmationDialog
import com.floflacards.app.presentation.component.ModernScreenTopAppBar
import com.floflacards.app.presentation.component.EmptyStateCard
import com.floflacards.app.presentation.component.SearchBar
import com.floflacards.app.presentation.component.getContentAlpha
import com.floflacards.app.presentation.component.getCardContainerColor
import com.floflacards.app.presentation.component.getCardBorder
import com.floflacards.app.presentation.component.csv.CsvExportSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFlashcards: (Long, String) -> Unit,
    onNavigateToCsvImport: () -> Unit = {},
    onNavigateToCsvExportAll: () -> Unit = {},
    onNavigateToCsvExport: (Long, String) -> Unit = { _, _ -> },
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categoryUiState by categoryViewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCsvExportDialog by remember { mutableStateOf(false) }
    
    // Get filtered categories based on search query
    val filteredCategories = categoryViewModel.getFilteredCategories()
    val hasSearchResults = filteredCategories.isNotEmpty()
    val isSearching = categoryUiState.searchQuery.isNotBlank()
    
    // Calculate bulk action state
    val bulkActionState = categoryViewModel.getBulkActionState()
    
    Scaffold(
        topBar = {
            ModernScreenTopAppBar(
                title = stringResource(R.string.settings_manage_categories_title),
                onNavigateBack = onNavigateBack,
                itemCount = categoryUiState.categories.size,
                activeCount = categoryUiState.categories.count { it.isEnabled },
                bulkActionState = bulkActionState,
                onEnableAll = { categoryViewModel.enableAllCategories() },
                onDisableAll = { categoryViewModel.disableAllCategories() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.settings_add_category)
                    )
                },
                text = { Text(stringResource(R.string.settings_add_category)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar - only show when categories exist
            SearchBar(
                query = categoryUiState.searchQuery,
                onQueryChange = { categoryViewModel.updateSearchQuery(it) },
                placeholder = stringResource(R.string.search_categories),
                visible = categoryUiState.categories.isNotEmpty() && !categoryUiState.isLoading
            )

            // CSV Import/Export buttons
            AnimatedVisibility(
                visible = categoryUiState.categories.isNotEmpty() && !categoryUiState.isLoading,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToCsvImport() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_import),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = { showCsvExportDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_export),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Content
            AnimatedVisibility(
                visible = categoryUiState.isLoading,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.settings_loading_categories),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = !categoryUiState.isLoading,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                when {
                    // No categories at all
                    categoryUiState.categories.isEmpty() -> {
                        EmptyStateCard(
                            title = stringResource(R.string.settings_no_categories_title),
                            description = stringResource(R.string.settings_no_categories_description),
                            buttonText = stringResource(R.string.settings_create_first_category),
                            onButtonClick = { showAddCategoryDialog = true },
                            icon = Icons.AutoMirrored.Outlined.List,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // Search with no results
                    isSearching && !hasSearchResults -> {
                        EmptyStateCard(
                            title = stringResource(R.string.search_no_results),
                            description = stringResource(R.string.search_no_results_description),
                            buttonText = stringResource(R.string.search_clear),
                            onButtonClick = { categoryViewModel.updateSearchQuery("") },
                            icon = Icons.Default.Search,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // Show filtered categories
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 80.dp)
                        ) {
                            items(
                                items = filteredCategories,
                                key = { it.id }
                            ) { category ->
                                ModernCategoryCard(
                                    category = category,
                                    flashcardCount = categoryUiState.categoryCounts[category.id] ?: 0,
                                    onToggleEnabled = { categoryViewModel.toggleCategoryEnabled(category) },
                                    onEdit = { selectedCategory = category },
                                    onDelete = { categoryViewModel.requestDeleteCategory(category) },
                                    onNavigateToFlashcards = { onNavigateToFlashcards(category.id, category.name) },
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onConfirm = { name ->
                categoryViewModel.createCategory(name)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }
    

    
    selectedCategory?.let { category ->
        EditCategoryDialog(
            category = category,
            onConfirm = { updatedCategory ->
                categoryViewModel.updateCategory(updatedCategory)
                selectedCategory = null
            },
            onDismiss = { selectedCategory = null }
        )
    }
    
    // Delete Category Confirmation Dialog
    categoryUiState.categoryToDelete?.let { category ->
        DeleteCategoryConfirmationDialog(
            categoryName = category.name,
            flashcardCount = categoryUiState.categoryFlashcardCount,
            onConfirm = { categoryViewModel.confirmDeleteCategory() },
            onDismiss = { categoryViewModel.cancelDeleteCategory() }
        )
    }

    // CSV Export Selection Dialog
    if (showCsvExportDialog) {
        CsvExportSelectionDialog(
            categories = categoryUiState.categories,
            onExportCategory = { categoryId, categoryName ->
                showCsvExportDialog = false
                onNavigateToCsvExport(categoryId, categoryName)
            },
            onExportAll = {
                showCsvExportDialog = false
                onNavigateToCsvExportAll()
            },
            onDismiss = { showCsvExportDialog = false }
        )
    }
}






/**
 * Compact category row: leading list icon, name + live card count, enable toggle, and an overflow
 * menu (edit / delete). The whole row is clickable to manage the category's flashcards. Replaces
 * the old tall card (status badge + creation date + boxed name + full-width button) so long lists
 * stay scannable.
 */
@Composable
fun ModernCategoryCard(
    category: CategoryEntity,
    flashcardCount: Int,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = getContentAlpha(category.isEnabled)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onNavigateToFlashcards,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getCardContainerColor(category.isEnabled)
        ),
        shape = RoundedCornerShape(16.dp),
        border = getCardBorder(category.isEnabled),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (category.isEnabled) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.stats_cards_suffix, flashcardCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }

            Switch(
                checked = category.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.category_more_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_edit_category)) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_delete_category)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}



@Composable
fun AddCategoryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    
    UnifiedDialog(
        title = stringResource(R.string.settings_add_new_category),
        confirmButtonText = stringResource(R.string.settings_add_category),
        onConfirm = { 
            if (categoryName.isNotBlank()) {
                onConfirm(categoryName)
            }
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text(stringResource(R.string.settings_category_name)) },
            singleLine = true
        )
    }
}

@Composable
fun EditCategoryDialog(
    category: CategoryEntity,
    onConfirm: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf(category.name) }
    
    UnifiedDialog(
        title = stringResource(R.string.settings_edit_category),
        confirmButtonText = stringResource(R.string.flashcard_save_button),
        onConfirm = { 
            if (categoryName.isNotBlank()) {
                onConfirm(category.copy(
                    name = categoryName,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text(stringResource(R.string.settings_category_name)) },
            singleLine = true
        )
    }
}


