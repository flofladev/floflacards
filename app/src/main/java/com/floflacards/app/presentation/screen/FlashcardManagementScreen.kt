package com.floflacards.app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.presentation.viewmodel.FlashcardViewModel
import com.floflacards.app.presentation.component.DeleteFlashcardConfirmationDialog
import com.floflacards.app.presentation.component.ModernScreenTopAppBar
import com.floflacards.app.presentation.component.EmptyStateCard
import com.floflacards.app.presentation.component.ModernFlashcardCard
import com.floflacards.app.presentation.component.SearchBar
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardManagementScreen(
    category: CategoryEntity,
    onNavigateBack: () -> Unit,
    onNavigateToAddFlashcard: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingFlashcard by remember { mutableStateOf<FlashcardEntity?>(null) }
    
    // Get filtered flashcards based on search query
    val filteredFlashcards = viewModel.getFilteredFlashcards()
    val hasSearchResults = filteredFlashcards.isNotEmpty()
    val isSearching = uiState.searchQuery.isNotBlank()
    
    // Calculate bulk action state
    val bulkActionState = viewModel.getBulkActionState()
    
    LaunchedEffect(category.id) {
        viewModel.loadFlashcardsByCategory(category.id)
    }
    
    Scaffold(
        topBar = {
            ModernScreenTopAppBar(
                title = category.name,
                onNavigateBack = onNavigateBack,
                itemCount = uiState.flashcards.size,
                activeCount = uiState.flashcards.count { it.isEnabled },
                bulkActionState = bulkActionState,
                onEnableAll = { viewModel.enableAllFlashcardsInCategory() },
                onDisableAll = { viewModel.disableAllFlashcardsInCategory() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddFlashcard,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.flashcard_management_add_description)
                    )
                },
                text = { Text(stringResource(R.string.flashcard_management_add_button)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar - only show when flashcards exist
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                placeholder = stringResource(R.string.search_flashcards),
                visible = uiState.flashcards.isNotEmpty() && !uiState.isLoading
            )
            
            // Content
            AnimatedVisibility(
                visible = uiState.isLoading,
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
                            text = stringResource(R.string.flashcard_management_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                when {
                    // No flashcards at all
                    uiState.flashcards.isEmpty() -> {
                        EmptyStateCard(
                            title = stringResource(R.string.flashcard_management_empty_title),
                            description = stringResource(R.string.flashcard_management_empty_description),
                            buttonText = stringResource(R.string.flashcard_management_empty_button),
                            onButtonClick = onNavigateToAddFlashcard,
                            icon = Icons.Outlined.Info,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // Search with no results
                    isSearching && !hasSearchResults -> {
                        EmptyStateCard(
                            title = stringResource(R.string.search_no_results),
                            description = stringResource(R.string.search_no_results_description),
                            buttonText = stringResource(R.string.search_clear),
                            onButtonClick = { viewModel.updateSearchQuery("") },
                            icon = Icons.Default.Search,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // Show filtered flashcards
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 80.dp)
                        ) {
                            items(
                                items = filteredFlashcards,
                                key = { it.id }
                            ) { flashcard ->
                                ModernFlashcardCard(
                                    flashcard = flashcard,
                                    onEdit = { editingFlashcard = flashcard },
                                    onDelete = { viewModel.requestDeleteFlashcard(flashcard) },
                                    onToggleEnabled = { viewModel.toggleFlashcardEnabled(flashcard) },
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show edit flashcard screen when editing
    editingFlashcard?.let { flashcard ->
        AddEditFlashcardScreen(
            categoryId = category.id,
            flashcardToEdit = flashcard,
            onNavigateBack = { editingFlashcard = null }
        )
    }
    
    // Delete Confirmation Dialog
    uiState.flashcardToDelete?.let { flashcard ->
        DeleteFlashcardConfirmationDialog(
            flashcardQuestion = flashcard.question,
            onConfirm = { viewModel.confirmDeleteFlashcard() },
            onDismiss = { viewModel.cancelDeleteFlashcard() }
        )
    }
    
    // Error handling
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
}


















