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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.presentation.component.statistics.*
import com.floflacards.app.presentation.component.SearchBar
import com.floflacards.app.presentation.component.EmptyStateCard
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.presentation.viewmodel.CategoryStats
import com.floflacards.app.presentation.viewmodel.FlashcardStats
import com.floflacards.app.presentation.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showFlashcardResetDialog by remember { mutableStateOf<FlashcardStats?>(null) }
    var showCategoryResetDialog by remember { mutableStateOf<CategoryStats?>(null) }
    
    // Get filtered category stats based on search query
    val filteredCategoryStats = viewModel.getFilteredCategoryStats()
    val hasSearchResults = filteredCategoryStats.isNotEmpty()
    val isSearching = uiState.searchQuery.isNotBlank()
    
    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }
    
    // Global reset confirmation dialog
    if (showResetDialog) {
        ResetAllStatisticsDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.resetAllStatistics()
                showResetDialog = false
            },
            currentStreak = uiState.overallStats?.streakDays ?: 0,
            highestStreak = uiState.overallStats?.highestStreak ?: 0,
            totalFlashcards = uiState.overallStats?.totalFlashcards ?: 0
        )
    }
    
    // Individual flashcard reset confirmation dialog
    showFlashcardResetDialog?.let { flashcard ->
        FlashcardResetConfirmationDialog(
            flashcard = flashcard,
            onDismiss = { showFlashcardResetDialog = null },
            onConfirm = {
                viewModel.resetFlashcardStatistics(flashcard.id)
                showFlashcardResetDialog = null
            }
        )
    }
    
    // Category reset confirmation dialog
    showCategoryResetDialog?.let { category ->
        CategoryResetConfirmationDialog(
            category = category,
            onDismiss = { showCategoryResetDialog = null },
            onConfirm = {
                viewModel.resetCategoryStatistics(category.categoryId)
                showCategoryResetDialog = null
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getStatisticsBackground())
    ) {
        // Modern Theme-Aware Top Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.statistics_title),
                    fontWeight = FontWeight.Medium,
                    color = getStatisticsOnSurface()
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.statistics_back),
                        tint = getStatisticsOnSurface()
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showResetDialog = true },
                    enabled = uiState.overallStats?.totalFlashcards ?: 0 > 0
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.statistics_reset_all),
                        tint = if (uiState.overallStats?.totalFlashcards ?: 0 > 0) 
                            getStatisticsOnSurface() else getStatisticsOnSurfaceVariant()
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = getStatisticsSurface()
            )
        )
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentPurple
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search bar - only show when statistics exist
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    placeholder = stringResource(R.string.search_statistics),
                    visible = uiState.categoryStats.isNotEmpty()
                )
                
                // Content - check for search results
                if (isSearching && !hasSearchResults) {
                    // No search results found
                    EmptyStateCard(
                        title = stringResource(R.string.search_no_results),
                        description = stringResource(R.string.search_no_results_description),
                        buttonText = stringResource(R.string.search_clear),
                        onButtonClick = { viewModel.updateSearchQuery("") },
                        icon = Icons.Default.Search,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Modern Statistics Card Grid - only show when not searching
                        if (!isSearching) {
                            item {
                                uiState.overallStats?.let { stats ->
                                    ModernStatsCardGrid(stats)
                                    
                                    // Add some space between the cards and category list
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                        
                        // Category Cards - use filtered stats
                        items(filteredCategoryStats) { categoryStats ->
                            ModernCategoryCard(
                                categoryStats = categoryStats,
                                onToggleExpansion = { viewModel.toggleCategoryExpansion(categoryStats.categoryId) },
                                onFlashcardResetClick = { flashcard -> showFlashcardResetDialog = flashcard },
                                onCategoryResetClick = { showCategoryResetDialog = categoryStats }
                            )
                        }
                    }
                }
            }
        }
    }
}

