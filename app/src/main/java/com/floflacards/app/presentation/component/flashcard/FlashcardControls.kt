package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floflacards.app.domain.model.FlashcardRating

/**
 * Controls component for rating buttons
 * Extracted from FlashcardOverlayComponents.kt to follow "avoid large files" rule
 * 
 * This component provides:
 * - Three rating buttons: Wrong (❌), Hard (❓), Good (✅)
 * - Consistent styling with intentional color scheme for flashcard rating context
 * - Minimalistic design with emoji icons
 * - Equal weight distribution across buttons
 */
@Composable
fun FlashcardControls(
    onRating: (FlashcardRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // Don't know at all - Minimalistic
            Button(
                onClick = { onRating(FlashcardRating.WRONG) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F), // Darker red
                    contentColor = Color.White
                ),
                shape = SharedStyles.CornerRadius.small,
                elevation = ButtonElevation.standard(),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text(
                    text = "❌",
                    fontSize = 18.sp
                )
            }
            
            // Know partially - Minimalistic
            Button(
                onClick = { onRating(FlashcardRating.HARD) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF57C00), // Darker orange
                    contentColor = Color.White
                ),
                shape = SharedStyles.CornerRadius.small,
                elevation = ButtonElevation.standard(),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text(
                    text = "❓",
                    fontSize = 18.sp
                )
            }
            
            // Know it well - Minimalistic
            Button(
                onClick = { onRating(FlashcardRating.GOOD) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF388E3C), // Darker green
                    contentColor = Color.White
                ),
                shape = SharedStyles.CornerRadius.small,
                elevation = ButtonElevation.standard(),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text(
                    text = "✅",
                    fontSize = 18.sp
                )
            }
    }
}
