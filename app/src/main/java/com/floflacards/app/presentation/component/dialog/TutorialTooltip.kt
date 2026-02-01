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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Tutorial Tooltip component following SRP.
 * Displays guided instructions with clean, modern UI.
 * Reusable across different tutorial steps.
 */
@Composable
fun TutorialTooltip(
    text: String,
    isVisible: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: (() -> Unit)? = null,
    showNextButton: Boolean = true,
    showBackButton: Boolean = false,
    nextButtonText: String = "Next",
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = modifier
                    .padding(16.dp)
                    .widthIn(max = 400.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tutorial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        
                        IconButton(
                            onClick = onSkip,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Skip tutorial",
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tutorial text
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = when {
                            showBackButton && showNextButton -> Arrangement.SpaceBetween
                            showNextButton -> Arrangement.SpaceBetween
                            else -> Arrangement.Center
                        }
                    ) {
                        // Back button (conditional)
                        if (showBackButton && onBack != null) {
                            OutlinedButton(
                                onClick = onBack,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2196F3)
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = "Back",
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        // Skip button
                        OutlinedButton(
                            onClick = onSkip,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF9E9E9E)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 14.sp
                            )
                        }
                        
                        // Next button (conditional)
                        if (showNextButton) {
                            Button(
                                onClick = onNext,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = nextButtonText,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tutorial overlay that dims the background and highlights interactive elements.
 * Follows KISS principle with minimal, focused functionality.
 */
@Composable
fun TutorialOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    )
}
