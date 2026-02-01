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

package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.floflacards.app.data.entity.FlashcardEntity
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R

/**
 * Dialog for adding a new flashcard with question and answer fields.
 * 
 * @param onConfirm Callback invoked when user confirms with valid question and answer
 * @param onDismiss Callback invoked when user dismisses the dialog
 */
@Composable
fun AddFlashcardDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    UnifiedDialog(
        title = stringResource(R.string.add_flashcard_dialog_title),
        confirmButtonText = stringResource(R.string.add_flashcard_dialog_confirm),
        onConfirm = {
            if (question.isNotBlank() && answer.isNotBlank()) {
                onConfirm(question, answer)
            }
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.flashcard_question_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text(stringResource(R.string.flashcard_answer_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

/**
 * Dialog for editing an existing flashcard with pre-populated question and answer fields.
 * 
 * @param flashcard The flashcard entity to edit
 * @param onConfirm Callback invoked when user confirms with updated flashcard
 * @param onDismiss Callback invoked when user dismisses the dialog
 */
@Composable
fun EditFlashcardDialog(
    flashcard: FlashcardEntity,
    onConfirm: (FlashcardEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember { mutableStateOf(flashcard.question) }
    var answer by remember { mutableStateOf(flashcard.answer) }
    val focusManager = LocalFocusManager.current
    
    UnifiedDialog(
        title = stringResource(R.string.edit_flashcard_dialog_title),
        confirmButtonText = stringResource(R.string.edit_flashcard_dialog_confirm),
        onConfirm = {
            if (question.isNotBlank() && answer.isNotBlank()) {
                onConfirm(flashcard.copy(
                    question = question,
                    answer = answer,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.flashcard_question_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text(stringResource(R.string.flashcard_answer_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}
