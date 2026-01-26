package com.floflacards.app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.domain.model.InteractionMode
import com.floflacards.app.presentation.component.text.AutoSizeText

/**
 * Helper function to get localized display name for InteractionMode
 */
@Composable
fun InteractionMode.getDisplayName(): String {
    return when (this) {
        InteractionMode.NORMAL -> stringResource(R.string.interaction_mode_learning)
        InteractionMode.DRAG -> stringResource(R.string.interaction_mode_drag)
        InteractionMode.RESIZE -> stringResource(R.string.interaction_mode_resize)
        InteractionMode.OPACITY -> stringResource(R.string.interaction_mode_opacity)
    }
}

/**
 * FlashcardModeSelector - Service-compatible modal for selecting interaction modes
 * Following SOLID principles - Single Responsibility for mode selection
 * 
 * Fixed version that works in Service context without Dialog wrapper
 * 
 * Features:
 * - 4 distinct mode cards (Normal, Drag, Resize, Opacity)
 * - Visual mode indicators with color coding
 * - Smooth animations and transitions
 * - Opacity controls integrated for opacity mode
 * - Service-compatible overlay design
 */
@Composable
fun FlashcardModeSelector(
    isVisible: Boolean,
    currentMode: InteractionMode,
    onModeSelected: (InteractionMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        // Semi-transparent backdrop that covers the entire overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.85f)
                    .clickable(enabled = false) { /* Prevent backdrop dismissal */ },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.interaction_mode_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Mode selection grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: Normal and Drag modes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModeCard(
                                mode = InteractionMode.NORMAL,
                                isSelected = currentMode == InteractionMode.NORMAL,
                                onSelected = onModeSelected,
                                modifier = Modifier.weight(1f)
                            )
                            
                            ModeCard(
                                mode = InteractionMode.DRAG,
                                isSelected = currentMode == InteractionMode.DRAG,
                                onSelected = onModeSelected,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Row 2: Resize and Opacity modes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModeCard(
                                mode = InteractionMode.RESIZE,
                                isSelected = currentMode == InteractionMode.RESIZE,
                                onSelected = onModeSelected,
                                modifier = Modifier.weight(1f)
                            )
                            
                            ModeCard(
                                mode = InteractionMode.OPACITY,
                                isSelected = currentMode == InteractionMode.OPACITY,
                                onSelected = onModeSelected,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Note: Opacity controls are now integrated directly into the flashcard
                    // when opacity mode is selected, providing better UX
                    
                    // Instructions text with auto text sizing for better readability
                    AutoSizeText(
                        text = when (currentMode) {
                            InteractionMode.NORMAL -> stringResource(R.string.interaction_mode_learning_instruction)
                            InteractionMode.DRAG -> stringResource(R.string.interaction_mode_drag_instruction)
                            InteractionMode.RESIZE -> stringResource(R.string.interaction_mode_resize_instruction)
                            InteractionMode.OPACITY -> stringResource(R.string.interaction_mode_opacity_instruction)
                        },
                        color = Color(0xFFBDBDBD),
                        minTextSize = 10.sp,
                        maxTextSize = 12.sp,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Individual mode card component
 */
@Composable
private fun ModeCard(
    mode: InteractionMode,
    isSelected: Boolean,
    onSelected: (InteractionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable { onSelected(mode) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(mode.getPrimaryColor()).copy(alpha = 0.2f)
            } else {
                Color(0xFF16213E).copy(alpha = 0.8f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp, 
                Color(mode.getPrimaryColor())
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mode icon
            Text(
                text = mode.icon,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Mode name with auto text sizing for visibility
            AutoSizeText(
                text = mode.getDisplayName(),
                color = if (isSelected) Color(mode.getPrimaryColor()) else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                minTextSize = 8.sp,
                maxTextSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Selected indicator
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(mode.getPrimaryColor()),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
