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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * OpacityControls component for managing flashcard transparency
 * Following SOLID principles - Single Responsibility for opacity control
 * 
 * This component provides:
 * - Real-time opacity slider with 10%-100% range
 * - Visual percentage display
 * - Live preview of opacity changes
 * - Validation ensuring minimum 10% opacity (user requirement)
 * - Modern Material Design styling
 * 
 * Why this design:
 * - KISS: Simple slider interface is intuitive
 * - DRY: Reusable component for opacity control
 * - Best practices: Uses Compose state management patterns
 */
@Composable
fun OpacityControls(
    currentOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for smooth slider interaction
    var sliderValue by remember(currentOpacity) { 
        mutableFloatStateOf(currentOpacity) 
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A3E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with opacity percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.opacity_control_label),
                    color = Color(0xFF2196F3), // Blue theme for opacity mode
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${(sliderValue * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Opacity slider
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
            
            // Opacity level indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.opacity_control_minimum),
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
                Text(
                    text = stringResource(R.string.opacity_control_maximum),
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
            
            // Description for opacity mode
            Text(
                text = stringResource(R.string.opacity_control_description),
                color = Color(0xFFBDBDBD),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
