package com.floflacards.app.presentation.component

// Removed isSystemInDarkTheme import - using MaterialTheme.colorScheme instead
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.floflacards.app.R
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.util.DateUtils
import java.io.File

/**
 * Management-specific color functions for flashcard management screen.
 * These are separate from overlay flashcard colors in FlashcardColors.kt
 * to maintain proper separation of concerns and avoid affecting actual flashcards.
 */

/**
 * Get question card colors specifically for management screen with theme awareness
 */
@Composable
private fun getManagementQuestionCardColors(isEnabled: Boolean = true): androidx.compose.material3.CardColors {
    return CardDefaults.cardColors(
        containerColor = if (isEnabled) {
            // Use theme-aware primary container color - automatically adapts to light/dark theme
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            //Maintain color identity but lighter/more transparent for disabled state
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        }
    )
}

/**
 * Get answer card colors specifically for management screen with theme awareness
 */
@Composable
private fun getManagementAnswerCardColors(isEnabled: Boolean = true): androidx.compose.material3.CardColors {
    return CardDefaults.cardColors(
        containerColor = if (isEnabled) {
            // Use theme-aware secondary container color - automatically adapts to light/dark theme
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        } else {
            //Maintain color identity but lighter/more transparent for disabled state
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        }
    )
}

/**
 * Get text color for management cards with theme awareness
 */
@Composable
private fun getManagementTextColor(isEnabled: Boolean = true, alpha: Float = 1f): Color {
    // Use theme-aware text colors - automatically adapts to light/dark theme
    return if (isEnabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.4f)
    }
}

/**
 * Get question label text color for management cards with theme awareness
 */
@Composable
private fun getManagementQuestionLabelColor(isEnabled: Boolean = true, alpha: Float = 1f): Color {
    // Use theme-aware primary color for question labels - automatically adapts to light/dark theme
    return if (isEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f)
    }
}

/**
 * Get answer label text color for management cards with theme awareness
 */
@Composable
private fun getManagementAnswerLabelColor(isEnabled: Boolean = true, alpha: Float = 1f): Color {
    // Use theme-aware secondary color for answer labels - automatically adapts to light/dark theme
    return if (isEnabled) {
        MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = alpha * 0.3f)
    }
}

/**
 * Modern flashcard card component following Material Design 3 principles.
 * Extracted from FlashcardManagementScreen to follow Single Responsibility Principle.
 * Uses existing shared components to avoid duplication.
 */
@Composable
fun ModernFlashcardCard(
    flashcard: FlashcardEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = getContentAlpha(flashcard.isEnabled)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (flashcard.isEnabled) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = getCardContainerColor(flashcard.isEnabled)
        ),
        shape = RoundedCornerShape(16.dp),
        border = getCardBorder(flashcard.isEnabled)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with status indicator
            FlashcardManagementHeader(
                isEnabled = flashcard.isEnabled,
                createdAt = flashcard.createdAt
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Question
            FlashcardQuestionCard(
                question = flashcard.question,
                isEnabled = flashcard.isEnabled,
                contentAlpha = contentAlpha,
                imagePath = flashcard.questionImagePath
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Answer
            FlashcardAnswerCard(
                answer = flashcard.answer,
                isEnabled = flashcard.isEnabled,
                contentAlpha = contentAlpha,
                answerImagePath = flashcard.answerImagePath
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action Buttons
            FlashcardActionButtons(
                isEnabled = flashcard.isEnabled,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleEnabled = onToggleEnabled,
                contentAlpha = contentAlpha
            )
        }
    }
}

/**
 * Flashcard management header with status badge and creation date.
 * Uses existing StatusBadge component to avoid duplication.
 * Renamed to avoid conflict with overlay FlashcardHeader.
 */
@Composable
fun FlashcardManagementHeader(
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (isEnabled) 0.6f else 0.35f
            )
        )
    }
}

/**
 * Thumbnail image component for management cards.
 * 
 * @param imagePath Path to the image
 * @param contentDescription Description for accessibility
 */
@Composable
private fun FlashcardThumbnail(
    imagePath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imagePath != null) {
        Box(
            modifier = modifier
                .size(80.dp)
                .padding(top = 8.dp)
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Question card component for management screen with theme-aware styling.
 * Uses management-specific colors separate from overlay flashcards.
 */
@Composable
fun FlashcardQuestionCard(
    question: String,
    isEnabled: Boolean,
    contentAlpha: Float,
    imagePath: String? = null
) {
    Card(
        colors = getManagementQuestionCardColors(isEnabled),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.flashcard_question_label),
                style = MaterialTheme.typography.labelSmall,
                color = getManagementQuestionLabelColor(isEnabled, contentAlpha),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium,
                color = getManagementTextColor(isEnabled, contentAlpha),
                fontWeight = FontWeight.Medium
            )
            
            // Show thumbnail if image exists
            FlashcardThumbnail(
                imagePath = imagePath,
                contentDescription = stringResource(R.string.flashcard_question_label)
            )
        }
    }
}

/**
 * Answer card component for management screen with theme-aware styling.
 * Uses management-specific colors separate from overlay flashcards.
 */
@Composable
fun FlashcardAnswerCard(
    answer: String,
    isEnabled: Boolean,
    contentAlpha: Float,
    answerImagePath: String? = null
) {
    Card(
        colors = getManagementAnswerCardColors(isEnabled),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.flashcard_answer_label),
                style = MaterialTheme.typography.labelSmall,
                color = getManagementAnswerLabelColor(isEnabled, contentAlpha),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = getManagementTextColor(isEnabled, contentAlpha)
            )
            
            // Show thumbnail if image exists
            FlashcardThumbnail(
                imagePath = answerImagePath,
                contentDescription = stringResource(R.string.flashcard_answer_label)
            )
        }
    }
}

/**
 * Action buttons for flashcard operations.
 * Uses existing ModernSquareIconButton to maintain consistency.
 */
@Composable
fun FlashcardActionButtons(
    isEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    contentAlpha: Float
) {
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
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        // Edit and Delete Buttons
        Row {
            ModernSquareIconButton(
                onClick = onEdit,
                icon = Icons.Outlined.Edit,
                contentDescription = "Edit flashcard",
                isEnabled = isEnabled,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            ModernSquareIconButton(
                onClick = onDelete,
                icon = Icons.Outlined.Delete,
                contentDescription = "Delete flashcard",
                isEnabled = isEnabled,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
    }
}
