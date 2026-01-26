package com.floflacards.app.presentation.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Resize handles component for corner/edge resizing
 * Extracted from FlashcardOverlayComponents.kt to follow "avoid large files" rule
 * 
 * This component provides:
 * - Bottom-right corner resize handle with drag gesture detection
 * - Visual feedback with size indicator overlay
 * - Consistent styling with orange resize handle
 */
@Composable
fun ResizeHandles(
    onSizeChange: (Int, Int) -> Unit,
    currentWidth: Int,
    currentHeight: Int,
    modifier: Modifier = Modifier
) {
    
    // Enhanced bottom-right corner resize handle
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Resize handle with enhanced styling
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .offset((-8).dp, (-8).dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        // Apply resize amount directly as relative change
                        // This will resize by the exact amount dragged
                        val widthChange = dragAmount.x.toInt()
                        val heightChange = dragAmount.y.toInt()
                        onSizeChange(widthChange, heightChange)
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF9800) // Orange background
            ),
            shape = SharedStyles.CornerRadius.small,
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⤡", // Better resize icon
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Size indicator overlay when resizing
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = SharedStyles.CornerRadius.small
        ) {
            Text(
                text = "${currentWidth} × ${currentHeight}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
