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

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.data.csv.CsvFlashcard
import com.floflacards.app.presentation.component.UnifiedDialog
import com.floflacards.app.presentation.component.EmptyStateCard
import com.floflacards.app.presentation.component.ContentCard
import com.floflacards.app.presentation.component.getCardContainerColor
import com.floflacards.app.presentation.component.getCardBorder
import com.floflacards.app.presentation.component.getHeaderContainerColor
import com.floflacards.app.presentation.component.getHeaderContentColor
import com.floflacards.app.presentation.viewmodel.CategoryViewModel
import com.floflacards.app.presentation.viewmodel.CsvImportViewModel
import com.floflacards.app.presentation.viewmodel.CsvImportUiState
import com.floflacards.app.presentation.viewmodel.ImportStep

/**
 * Sources the import flow can read from. CSV is the only one implemented today; others are shown
 * as "coming soon" so the format-selection step is meaningful and easy to extend (e.g. JSON).
 */
private enum class ImportFormat { CSV }

/**
 * Resolves a human-readable file name for a SAF document URI via [OpenableColumns.DISPLAY_NAME].
 * SAF URIs' last path segment is an opaque document id (e.g. "primary:Download/x.csv"), so it must
 * not be shown directly. Returns null if the name can't be resolved.
 */
private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()
}

/**
 * Import screen. First step lets the user pick a source format; once chosen, it runs that format's
 * flow (currently the CSV pick file -> preview -> import stepper).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoryUiState by categoryViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var skipDuplicates by remember { mutableStateOf(true) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedFormat by remember { mutableStateOf<ImportFormat?>(null) }
    var useFileCategories by remember { mutableStateOf(true) }

    // Auto-select first category when loaded
    LaunchedEffect(categoryUiState.categories) {
        if (selectedCategoryId == null && categoryUiState.categories.isNotEmpty()) {
            selectedCategoryId = categoryUiState.categories.first().id
        }
    }

    // SAF file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            val name = queryDisplayName(context.contentResolver, it)
                ?: it.lastPathSegment?.substringAfterLast('/')
                ?: context.getString(R.string.csv_import_selected_file_fallback)
            selectedFileName = name
            viewModel.setFile(it, name, context.contentResolver)
            viewModel.parseForPreview(context.contentResolver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (selectedFormat == null) R.string.action_import
                            else R.string.csv_import_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // From the CSV file-selection step, back returns to the format picker;
                        // anywhere else it leaves the import screen.
                        if (selectedFormat != null && uiState.step == ImportStep.IDLE) {
                            selectedFormat = null
                        } else {
                            viewModel.reset()
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.csv_import_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedFormat == null) {
                FormatSelectionStep(
                    onSelectCsv = { selectedFormat = ImportFormat.CSV },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            when (uiState.step) {
                ImportStep.IDLE -> {
                    FileSelectionStep(
                        onPickFile = {
                            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/tab-separated-values", "text/*"))
                        },
                        fileName = selectedFileName ?: uiState.fileName,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                ImportStep.PARSING -> LoadingStep()
                ImportStep.PREVIEW_READY -> {
                    uiState.parseResult?.let { result ->
                        val selectedCatId = selectedCategoryId
                        if (selectedCatId != null) {
                            val fileHasCategories = result.validCards.any { !it.category.isNullOrBlank() }
                            PreviewStep(
                                validCards = result.validCards,
                                errorCount = result.errors.size,
                                duplicateCount = uiState.duplicateCount,
                                skipDuplicates = skipDuplicates,
                                onSkipDuplicatesChanged = { skipDuplicates = it },
                                fileHasCategories = fileHasCategories,
                                useFileCategories = useFileCategories,
                                onUseFileCategoriesChanged = { useFileCategories = it },
                                selectedCategoryId = selectedCatId,
                                categories = categoryUiState.categories,
                                onCategoryChanged = { selectedCategoryId = it },
                                onImport = {
                                    viewModel.executeImport(
                                        categoryId = selectedCatId,
                                        skipDuplicates = skipDuplicates,
                                        resolveCategories = fileHasCategories && useFileCategories
                                    )
                                },
                                onPickAnotherFile = {
                                    filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/tab-separated-values", "text/*"))
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                ImportStep.NO_VALID_CARDS -> {
                    NoValidCardsStep(
                        onPickAnotherFile = {
                            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/tab-separated-values", "text/*"))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                ImportStep.IMPORTING -> ImportingStep()
                ImportStep.IMPORT_COMPLETE -> {
                    uiState.importResult?.let { result ->
                        ImportCompleteStep(
                            result = result,
                            onDone = {
                                viewModel.reset()
                                onNavigateBack()
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                ImportStep.ERROR -> {
                    ErrorStep(
                        error = uiState.error ?: stringResource(R.string.csv_import_unknown_error),
                        onClose = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatSelectionStep(
    onSelectCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.import_choose_source),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.import_choose_source_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // CSV — the only implemented source today.
        FormatCard(
            icon = Icons.Filled.Description,
            name = "CSV",
            subtitle = stringResource(R.string.import_format_csv_subtitle),
            enabled = true,
            onClick = onSelectCsv
        )

        Spacer(modifier = Modifier.height(12.dp))

        // JSON — placeholder so the source choice is meaningful and easy to enable later.
        FormatCard(
            icon = Icons.Filled.Code,
            name = "JSON",
            subtitle = stringResource(R.string.import_format_coming_soon),
            enabled = false,
            onClick = {}
        )
    }
}

@Composable
private fun FormatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = getCardContainerColor(isEnabled = enabled)
        ),
        border = getCardBorder(isEnabled = enabled),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
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
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FileSelectionStep(
    onPickFile: () -> Unit,
    fileName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.csv_import_select_file),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.csv_import_file_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.csv_import_supported_formats),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.csv_import_pick_file_button))
        }

        AnimatedVisibility(visible = fileName != null) {
            fileName?.let { name ->
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = getCardContainerColor(isEnabled = true)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = getCardBorder(isEnabled = true),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingStep() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.csv_import_parsing_file),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewStep(
    validCards: List<CsvFlashcard>,
    errorCount: Int,
    duplicateCount: Int,
    skipDuplicates: Boolean,
    onSkipDuplicatesChanged: (Boolean) -> Unit,
    fileHasCategories: Boolean,
    useFileCategories: Boolean,
    onUseFileCategoriesChanged: (Boolean) -> Unit,
    selectedCategoryId: Long,
    categories: List<com.floflacards.app.data.entity.CategoryEntity>,
    onCategoryChanged: (Long) -> Unit,
    onImport: () -> Unit,
    onPickAnotherFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable content area
        Column(
            modifier = modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = getHeaderContainerColor()
                ),
                shape = RoundedCornerShape(16.dp),
                border = getCardBorder(isEnabled = true),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.csv_import_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getHeaderContentColor()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.csv_import_valid_cards, validCards.size), style = MaterialTheme.typography.bodyMedium)
                        }

                        if (errorCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.csv_import_parse_errors, errorCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Duplicates already in the collection (skipped on import unless the toggle is off)
                    if (duplicateCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.csv_import_duplicates_count, duplicateCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Use-categories-from-file toggle (only when the file actually carries category info)
            if (fileHasCategories) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = useFileCategories, onCheckedChange = onUseFileCategoriesChanged)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = stringResource(R.string.csv_import_use_file_categories), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(text = stringResource(R.string.csv_import_use_file_categories_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Category selector (the target category, or the fallback when using file categories)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.csv_import_into_category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category chips for selection — FlowRow so many/long names wrap instead of clipping
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = category.id == selectedCategoryId,
                        onClick = { onCategoryChanged(category.id) },
                        label = { Text(category.name, maxLines = 1) },
                        leadingIcon = if (category.id == selectedCategoryId) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            // Preview table
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.csv_import_preview_label, minOf(validCards.size, 10), validCards.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Table header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stringResource(R.string.csv_import_question_header), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = stringResource(R.string.csv_import_answer_header), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Preview rows using ContentCard for consistent styling
            validCards.take(10).forEachIndexed { index, card ->
                ContentCard(
                    isEnabled = true,
                    primaryContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = card.question, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(text = card.answer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (index < 9) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (validCards.size > 10) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.csv_import_and_more, validCards.size - 10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = skipDuplicates, onCheckedChange = onSkipDuplicatesChanged)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = stringResource(R.string.csv_import_skip_duplicates), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(text = stringResource(R.string.csv_import_skip_duplicates_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sticky bottom bar with action buttons
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Import button
                // Reflect how many will actually be imported: duplicates are skipped unless the
                // "skip duplicates" toggle is off.
                val importCount = if (skipDuplicates) {
                    (validCards.size - duplicateCount).coerceAtLeast(0)
                } else {
                    validCards.size
                }
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = importCount > 0
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.csv_import_import_button, importCount))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onPickAnotherFile, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.csv_import_pick_another))
                }
            }
        }
    }
}

@Composable
private fun NoValidCardsStep(onPickAnotherFile: () -> Unit, modifier: Modifier = Modifier) {
    EmptyStateCard(
        title = stringResource(R.string.csv_import_no_valid_cards),
        description = stringResource(R.string.csv_import_supported_formats),
        buttonText = stringResource(R.string.csv_import_pick_another),
        onButtonClick = onPickAnotherFile,
        icon = Icons.Default.Warning,
        modifier = modifier.then(Modifier.padding(16.dp))
    )
}

@Composable
private fun ImportingStep() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.csv_import_importing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportCompleteStep(
    result: com.floflacards.app.data.csv.CsvImportResult,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.csv_import_done),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = getHeaderContainerColor()
            ),
            shape = RoundedCornerShape(16.dp),
            border = getCardBorder(isEnabled = true),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = stringResource(R.string.csv_import_success, result.successCount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

                if (result.skippedCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.csv_import_skipped, result.skippedCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (result.errorCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.csv_import_parsing_errors_detail, result.errorCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.csv_import_done_button))
        }
    }
}

@Composable
private fun ErrorStep(
    error: String,
    onClose: () -> Unit
) {
    UnifiedDialog(
        title = stringResource(R.string.csv_import_error_title),
        confirmButtonText = stringResource(R.string.csv_import_error_close),
        onConfirm = onClose,
        onDismiss = onClose
    ) {
        Text(error)
    }
}
