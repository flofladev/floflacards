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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.floflacards.app.R
import com.floflacards.app.domain.model.InteractionMode

/**
 * CompactOpacitySlider - Bottom overlay slider for flashcard opacity control
 * Following SOLID principles - Single Responsibility for compact opacity control
 * 
 * This component provides:
 * - Compact horizontal slider with minimal height
 * - Real-time opacity percentage display
 * - Smooth slide-in/out animations
 * - Modern Material Design styling optimized for overlay use
 * - Touch-friendly slider with proper sizing
 * 
 * Why this design:
 * - KISS: Simple, always-visible slider when needed
 * - DRY: Reusable compact component
 * - UX: Non-intrusive bottom placement
 * - Performance: Lightweight with smooth animations
 */
@Composable
fun CompactOpacitySlider(
    isVisible: Boolean,
    currentOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for smooth slider interaction
    var sliderValue by remember(currentOpacity) { 
        mutableFloatStateOf(currentOpacity) 
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Opacity label above slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.opacity_control_label),
                        color = Color(0xFF2196F3), // Blue theme for opacity mode
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Percentage display
                    Text(
                        text = "${(sliderValue * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Compact slider below label
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        // Validate and update local state immediately for smooth UI
                        val validatedValue = InteractionMode.validateOpacity(newValue)
                        sliderValue = validatedValue
                        onOpacityChange(validatedValue)
                    },
                    valueRange = 0.1f..1.0f, // 10% to 100%
                    steps = 17, // 18 steps total (10%, 15%, 20%, ..., 100%)
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3),
                        inactiveTrackColor = Color(0xFF2196F3).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
