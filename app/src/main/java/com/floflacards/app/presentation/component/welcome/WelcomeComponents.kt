package com.floflacards.app.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable components for welcome onboarding flow.
 * Extracted from WelcomeScreen.kt following DRY principle.
 * Follows SOLID principles - each component has single responsibility.
 * Follows KISS principle - simple, focused component implementations.
 */

/**
 * Permission status indicator showing granted/denied state with appropriate styling.
 * Reusable across multiple permission steps.
 */
@Composable
fun PermissionStatusIndicator(
    isGranted: Boolean,
    grantedText: String,
    deniedText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = if (isGranted) "Granted" else "Required",
            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        
        Text(
            text = if (isGranted) grantedText else deniedText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    }
}

/**
 * Standardized welcome step card component.
 * Provides consistent layout and styling across all onboarding steps.
 * Supports optional secondary button for complex interactions.
 * Enhanced with better accessibility and visual separation.
 * Added support for top-right action (e.g., language selection).
 */
@Composable
fun WelcomeStepCard(
    title: String,
    content: @Composable () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit,
    isButtonEnabled: Boolean,
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
    topRightAction: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), // Slightly more rounded for modern look
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp, // Slightly more elevation for better depth
            pressedElevation = 8.dp
        )
    ) {
        // Box to allow absolute positioning of top-right action
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp), // Increased padding for better breathing room
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with enhanced accessibility
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
                
                // Content with improved spacing
                content()
                
                // Visual separator before buttons
                Spacer(modifier = Modifier.height(36.dp))
                
                // Primary button with enhanced styling
                Button(
                    onClick = onButtonClick,
                    enabled = isButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        disabledContainerColor = Color(0xFFBDBDBD),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp), // Consistent with card rounding
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp // Better text spacing
                    )
                }
                
                // Secondary button with improved styling (optional)
                if (secondaryButtonText != null && onSecondaryButtonClick != null) {
                    Spacer(modifier = Modifier.height(16.dp)) // Better spacing between buttons
                    
                    OutlinedButton(
                        onClick = onSecondaryButtonClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = secondaryButtonText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.25.sp
                        )
                    }
                }
            }
            
            // Top-right action (e.g., language selection)
            if (topRightAction != null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    topRightAction()
                }
            }
        }
    }
}
