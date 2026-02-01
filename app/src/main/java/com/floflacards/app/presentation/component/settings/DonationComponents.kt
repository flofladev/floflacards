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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Professional donation dialog following Material Design 3 principles.
 * Matches app's existing design patterns and user's privacy requirements.
 * Follows DRY, KISS, and SOLID principles.
 */
@Composable
fun DonationDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    UnifiedDialog(
        title = "Support Development",
        confirmButtonText = "Close",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    ) {
        Column {
            Text(
                text = "Thank you for using Floating Learning! Your support helps keep this app free and continuously improving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Tab Selection
            DonationTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> PayPalDonationContent()
                1 -> CryptoDonationContent()
            }
        }
    }
}

/**
 * Tab row for donation methods selection.
 * Uses app's consistent color scheme.
 */
@Composable
private fun DonationTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DonationTab(
            title = "PayPal",
            icon = Icons.Filled.Favorite,
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )
        
        DonationTab(
            title = "Crypto",
            icon = Icons.Filled.Star,
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual donation tab component.
 * Follows app's button styling patterns.
 */
@Composable
private fun DonationTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                getDonationTabSelectedColor() 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) 
                    getDonationTabSelectedContentColor() 
                else 
                    getDonationIconColor() // Use theme-aware icon color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) 
                    getDonationTabSelectedContentColor() 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * PayPal donation content with privacy-first approach.
 * No tracking, just external link opening.
 */
@Composable
private fun PayPalDonationContent() {
    val context = LocalContext.current
    
    Column {
        Text(
            text = "Support via PayPal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Text(
            text = "Secure and trusted payment processing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // PayPal donation button
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/ncp/payment/J4UM2ZTS2GZJL"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0070BA), // PayPal blue
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Donate via PayPal",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Cryptocurrency donation content with copy-to-clipboard functionality.
 * Provides maximum privacy for donations.
 */
@Composable
private fun CryptoDonationContent() {
    val clipboardManager = LocalClipboardManager.current
    var copiedAddress by remember { mutableStateOf<String?>(null) }
    
    // Sample addresses (user can replace these)
    val cryptoAddresses = listOf(
        CryptoAddress("Monero", "49XpRSnCVS6So6ZY4KYGP45F6KYRiKPWjVgWe4PY82woDPSBZFMEbXFhPsfsVXH3zsdMZRZPxM2L7PiFKSn86fP9UnAJx9p"),
        CryptoAddress("Ethereum", "0x8467e6B9F9E502F5352C153FeAb03e456eD1dcc9")
    )
    
    // Reset copied state after delay
    LaunchedEffect(copiedAddress) {
        if (copiedAddress != null) {
            kotlinx.coroutines.delay(2000)
            copiedAddress = null
        }
    }
    
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Cryptocurrency Donations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Text(
            text = "Direct and private support with crypto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        cryptoAddresses.forEach { address ->
            CryptoAddressCard(
                crypto = address,
                isCopied = copiedAddress == address.currency,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(address.address))
                    copiedAddress = address.currency
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Individual cryptocurrency address card.
 * Follows app's card design patterns.
 */
@Composable
private fun CryptoAddressCard(
    crypto: CryptoAddress,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = crypto.currency,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                FilledTonalButton(
                    onClick = onCopy,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isCopied) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Copy address",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "Copied!" else "Copy",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = crypto.address,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            )
        }
    }
}

/**
 * Data class for cryptocurrency addresses.
 * Simple and clean structure.
 */
private data class CryptoAddress(
    val currency: String,
    val address: String
)
