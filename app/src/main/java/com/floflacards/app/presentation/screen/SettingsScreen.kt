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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
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
import com.floflacards.app.presentation.component.StatusBadge
import com.floflacards.app.presentation.component.ContentCard
import com.floflacards.app.presentation.component.ModernSquareIconButton
import com.floflacards.app.presentation.component.SearchBar
import com.floflacards.app.presentation.component.getContentAlpha
import com.floflacards.app.presentation.component.getCardContainerColor
import com.floflacards.app.presentation.component.getCardBorder
import com.floflacards.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFlashcards: (Long, String) -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categoryUiState by categoryViewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    
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
}






@Composable
fun ModernCategoryCard(
    category: CategoryEntity,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = getContentAlpha(category.isEnabled)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (category.isEnabled) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = getCardContainerColor(category.isEnabled)
        ),
        shape = RoundedCornerShape(16.dp),
        border = getCardBorder(category.isEnabled)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with status indicator
            CategoryHeader(
                isEnabled = category.isEnabled,
                createdAt = category.createdAt
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category name
            CategoryNameCard(
                categoryName = category.name,
                isEnabled = category.isEnabled,
                contentAlpha = contentAlpha
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action Buttons
            CategoryActionButtons(
                isEnabled = category.isEnabled,
                onNavigateToFlashcards = onNavigateToFlashcards,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleEnabled = onToggleEnabled,
                contentAlpha = contentAlpha
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    isEnabled: Boolean,
    createdAt: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusBadge(isEnabled = isEnabled)
        
        Text(
            text = DateUtils.formatDateTime(createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isEnabled) 0.6f else 0.35f
            )
        )
    }
}

@Composable
private fun CategoryNameCard(
    categoryName: String,
    isEnabled: Boolean,
    contentAlpha: Float
) {
    ContentCard(
        isEnabled = isEnabled,
        primaryContainerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.List,
                contentDescription = null,
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategoryActionButtons(
    isEnabled: Boolean,
    onNavigateToFlashcards: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    contentAlpha: Float
) {
    Column {
        // Manage Flashcards Button
        Button(
            onClick = onNavigateToFlashcards,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                contentColor = if (isEnabled)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_manage_flashcards))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Control Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enable/Disable Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
            
            // Edit and Delete Buttons
            Row {
                ModernSquareIconButton(
                    onClick = onEdit,
                    icon = Icons.Outlined.Edit,
                    contentDescription = "Edit category",
                    isEnabled = isEnabled,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                ModernSquareIconButton(
                    onClick = onDelete,
                    icon = Icons.Outlined.Delete,
                    contentDescription = "Delete category",
                    isEnabled = isEnabled,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
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


