package com.floflacards.app.presentation.screen

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floflacards.app.R
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.presentation.component.FlashcardImageSection
import com.floflacards.app.presentation.viewmodel.FlashcardViewModel
import kotlinx.coroutines.launch

/**
 * Add/Edit flashcard screen with proper IME autocorrect handling.
 * 
 * Key Features:
 * - TextFieldValue state for correct IME composition (prevents autocorrect issues)
 * - Auto-expanding text fields with unlimited height
 * - Autocorrect and emoji support enabled
 * - Unsaved changes detection with confirmation dialog
 * - Save button in app bar with loading state
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEditFlashcardScreen(
    categoryId: Long,
    flashcardToEdit: FlashcardEntity? = null,
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    // TextFieldValue preserves IME composition state (underlined autocorrect text)
    // This ensures autocorrect commits properly before newlines are inserted
    var questionText by remember {
        mutableStateOf(TextFieldValue(flashcardToEdit?.question ?: ""))
    }
    var answerText by remember {
        mutableStateOf(TextFieldValue(flashcardToEdit?.answer ?: ""))
    }
    
    // Initialize image state from existing flashcard or ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    
    // Set existing images when editing
    LaunchedEffect(flashcardToEdit) {
        if (flashcardToEdit != null) {
            viewModel.setExistingImages(
                flashcardToEdit.questionImagePath,
                flashcardToEdit.answerImagePath
            )
        }
    }
    
    var isSaving by remember { mutableStateOf(false) }
    
    // Unsaved changes detection
    val originalQuestion = flashcardToEdit?.question ?: ""
    val originalAnswer = flashcardToEdit?.answer ?: ""
    val hasUnsavedChanges = questionText.text.trim() != originalQuestion || answerText.text.trim() != originalAnswer
    
    // Dialog state for unsaved changes confirmation
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    val isEditing = flashcardToEdit != null
    val title = if (isEditing) stringResource(R.string.edit_flashcard_title) else stringResource(R.string.add_flashcard_title)
    
    // Image pickers
    var isQuestionImagePicker by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.saveImageFromUri(it, isQuestionImagePicker)
        }
    }
    
    val canSave = questionText.text.isNotBlank() && answerText.text.isNotBlank() && !isSaving
    
    // Scroll text field into view when keyboard appears
    val questionBringIntoViewRequester = remember { BringIntoViewRequester() }
    val answerBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    
    // Handle navigation with unsaved changes check
    val handleNavigateBack = {
        if (hasUnsavedChanges && (questionText.text.isNotBlank() || answerText.text.isNotBlank())) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }
    
    // Handle save action with saving state
    val handleSave = {
        if (canSave) {
            isSaving = true
            if (isEditing) {
                val updatedFlashcard = flashcardToEdit!!.copy(
                    question = questionText.text.trim(),
                    answer = answerText.text.trim(),
                    questionImagePath = uiState.tempQuestionImagePath,
                    answerImagePath = uiState.tempAnswerImagePath,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updateFlashcard(updatedFlashcard)
            } else {
                viewModel.createFlashcard(
                    categoryId, 
                    questionText.text.trim(), 
                    answerText.text.trim(),
                    uiState.tempQuestionImagePath,
                    uiState.tempAnswerImagePath
                )
            }
            onNavigateBack()
        }
    }
    
    // Handle save and exit from dialog
    val handleSaveAndExit = {
        handleSave()
        showUnsavedChangesDialog = false
    }
    
    // Handle discard changes
    val handleDiscardChanges = {
        showUnsavedChangesDialog = false
        onNavigateBack()
    }
    
    // Intercept system back button to check for unsaved changes
    BackHandler {
        handleNavigateBack()
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Adjust layout when keyboard appears
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.flashcard_back_button)
                        )
                    }
                },
                actions = {
                    // Save button in top right corner with saving state
                    IconButton(
                        onClick = handleSave,
                        enabled = canSave
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.flashcard_save_button),
                                tint = if (canSave) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Question input field
            FlashcardInputCard(
                value = questionText,
                onValueChange = { questionText = it },
                label = stringResource(R.string.flashcard_question_label),
                placeholder = stringResource(R.string.flashcard_question_placeholder),
                imagePath = uiState.tempQuestionImagePath,
                onAddImage = {
                    isQuestionImagePicker = true
                    imagePickerLauncher.launch("image/*")
                },
                onRemoveImage = { viewModel.removeTempImage(isQuestion = true) },
                bringIntoViewRequester = questionBringIntoViewRequester,
                coroutineScope = coroutineScope
            )
            
            // Answer input field
            FlashcardInputCard(
                value = answerText,
                onValueChange = { answerText = it },
                label = stringResource(R.string.flashcard_answer_label),
                placeholder = stringResource(R.string.flashcard_answer_placeholder),
                imagePath = uiState.tempAnswerImagePath,
                onAddImage = {
                    isQuestionImagePicker = false
                    imagePickerLauncher.launch("image/*")
                },
                onRemoveImage = { viewModel.removeTempImage(isQuestion = false) },
                bringIntoViewRequester = answerBringIntoViewRequester,
                coroutineScope = coroutineScope
            )
            
            // Bottom spacing for better scrolling experience
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Unsaved changes confirmation dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.flashcard_unsaved_changes_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.flashcard_unsaved_changes_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = handleSaveAndExit,
                    enabled = canSave
                ) {
                    Text(
                        text = stringResource(R.string.flashcard_save_changes),
                        color = if (canSave) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = handleDiscardChanges
                ) {
                    Text(
                        text = stringResource(R.string.flashcard_discard_changes),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Multi-line text input card with proper IME autocorrect handling.
 *
 * Implementation details:
 * - Uses TextFieldValue to track IME composition state (underlined autocorrect text)
 * - Implements "pending newline" pattern to handle Enter key during autocorrect:
 *   1. User presses Enter while word is composing (underlined)
 *   2. We mark a pending newline flag
 *   3. IME commits the autocorrected word
 *   4. We detect composition is done and insert the newline
 * - This ensures "acually" becomes "actually\n" not "acually\nactually"
 *
 * @param value Current text with composition state
 * @param onValueChange Callback when text changes
 * @param label Label text for the TextField
 * @param placeholder Placeholder text for the TextField
 * @param bringIntoViewRequester Scroll the field into view when focused
 * @param coroutineScope Scope for launching scroll animations
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlashcardInputCard(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String,
    imagePath: String?,
    onAddImage: () -> Unit,
    onRemoveImage: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        
        // Tracks when user presses Enter during autocorrect composition
        var pendingNewline by remember { mutableStateOf(false) }
        
        TextField(
            value = value,
            onValueChange = { incoming ->
                // Auto-insert newline after IME commits the autocorrected word
                if (pendingNewline && incoming.composition == null) {
                    val start = incoming.selection.start.coerceIn(0, incoming.text.length)
                    val end = incoming.selection.end.coerceIn(0, incoming.text.length)
                    val before = incoming.text.substring(0, start)
                    val after = incoming.text.substring(end)
                    val newText = before + "\n" + after
                    val newCursor = start + 1
                    pendingNewline = false
                    onValueChange(incoming.copy(text = newText, selection = TextRange(newCursor)))
                } else {
                    onValueChange(incoming)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
                .onKeyEvent { event: ComposeKeyEvent ->
                    // Detect Enter key press during autocorrect composition
                    val native = event.nativeKeyEvent
                    if (native.action == AndroidKeyEvent.ACTION_DOWN && 
                        native.keyCode == AndroidKeyEvent.KEYCODE_ENTER) {
                        if (value.composition != null) {
                            // Mark that we owe a newline after autocorrect commits
                            pendingNewline = true
                        }
                    }
                    false // Don't consume - let IME handle composition
                },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            minLines = 1,
            maxLines = Int.MAX_VALUE, // Auto-expand as user types
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = true, // Enable autocorrect and emoji support
                imeAction = ImeAction.Done // Encourages IME to commit composition
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (value.composition != null) {
                        // Composition still active - defer newline until it commits
                        pendingNewline = true
                    } else {
                        // No composition - insert newline immediately at cursor position
                        val start = value.selection.start.coerceIn(0, value.text.length)
                        val end = value.selection.end.coerceIn(0, value.text.length)
                        val newText = value.text.substring(0, start) + "\n" + value.text.substring(end)
                        val newCursor = start + 1
                        onValueChange(value.copy(text = newText, selection = TextRange(newCursor)))
                        keyboardController?.show() // Keep keyboard open for multi-line input
                    }
                }
            )
        )
        
        // Image section below text field
        FlashcardImageSection(
            imagePath = imagePath,
            onAddImage = onAddImage,
            onRemoveImage = onRemoveImage,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
