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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.presentation.component.statistics.*
import com.floflacards.app.presentation.viewmodel.FlashcardStats
import com.floflacards.app.presentation.viewmodel.StatisticsViewModel

/**
 * Per-category statistics detail. Replaces the old in-place accordion: tapping a category row in
 * [StatisticsScreen] opens this screen, which has its own full scroll space for the flashcard
 * breakdown (with a legend for the red/amber/green chips) plus reset actions.
 *
 * It reuses [StatisticsViewModel]; the relevant category is located by id from the loaded stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryStatsDetailScreen(
    categoryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFlashcardResetDialog by remember { mutableStateOf<FlashcardStats?>(null) }
    var showCategoryResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId) {
        viewModel.loadCategoryName(categoryId)
        viewModel.loadStatistics()
    }

    val category = uiState.categoryStats.find { it.categoryId == categoryId }
    // Title comes from the fast id lookup; fall back to the stats row once it loads.
    val categoryName = uiState.selectedCategoryName ?: category?.categoryName ?: ""
    val flashcards = category?.flashcards ?: emptyList()
    val hasResettableStats = flashcards.any { it.totalAttempts > 0 }

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
    if (showCategoryResetDialog && category != null) {
        CategoryResetConfirmationDialog(
            category = category,
            onDismiss = { showCategoryResetDialog = false },
            onConfirm = {
                viewModel.resetCategoryStatistics(categoryId)
                showCategoryResetDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getStatisticsBackground())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = categoryName,
                    fontWeight = FontWeight.Medium,
                    color = getStatisticsOnSurface(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                if (hasResettableStats) {
                    IconButton(onClick = { showCategoryResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.stats_reset_category_description),
                            tint = getStatisticsOnSurface()
                        )
                    }
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
                CircularProgressIndicator(color = AccentPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    StatLegend()
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (flashcards.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.stats_detail_empty),
                            color = getStatisticsOnSurfaceVariant(),
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    items(flashcards) { flashcard ->
                        FlashcardStatItem(
                            flashcard = flashcard,
                            onResetClick = { showFlashcardResetDialog = flashcard }
                        )
                    }
                }
            }
        }
    }
}
