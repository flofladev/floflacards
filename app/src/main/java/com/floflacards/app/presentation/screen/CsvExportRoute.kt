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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.presentation.viewmodel.CsvExportViewModel
import com.floflacards.app.presentation.viewmodel.CsvExportUiState

/**
 * Route composable for CSV export.
 * Shows a proper UI with loading state, then file picker, then export.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvExportRoute(
    categoryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CsvExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // The route only carries the category id; resolve the display name from it.
    LaunchedEffect(categoryId) {
        viewModel.loadCategory(categoryId)
    }

    // SAF create document launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportCategory(categoryId, context.contentResolver, it)
        } ?: onNavigateBack()
    }

    // Handle export completion
    LaunchedEffect(uiState.exportComplete) {
        if (uiState.exportComplete) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.csv_export_success, uiState.exportedCount)
            )
            viewModel.reset()
            onNavigateBack()
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Launch the file picker once the category name has loaded, suggesting a safe filename.
    // The name is sanitized so characters that are illegal in filenames (e.g. a "/" in the
    // category name) don't produce a broken suggestion.
    var pickerLaunched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.categoryName) {
        val name = uiState.categoryName
        if (name != null && !pickerLaunched) {
            pickerLaunched = true
            val safeName = sanitizeFilename(
                context.getString(R.string.csv_export_default_filename, name)
            )
            saveFileLauncher.launch("$safeName.csv")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.csv_export_title))
                        Text(
                            text = uiState.categoryName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.csv_import_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.csv_export_preparing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.csv_export_preparing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.csv_export_select_location),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Strips characters that are illegal or problematic in filenames (path separators, reserved
 * characters, control chars) and collapses whitespace to underscores, so a category name like
 * "Spanish/English" yields a valid suggested filename instead of a broken path.
 */
private fun sanitizeFilename(raw: String): String {
    val cleaned = raw
        .replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_")
        .replace(Regex("\\s+"), "_")
        .trim('_', '.')
    return cleaned.ifBlank { "flashcards" }
}
