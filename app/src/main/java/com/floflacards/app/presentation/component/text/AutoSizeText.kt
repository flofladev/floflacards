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

package com.floflacards.app.presentation.component.text

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * A composable that automatically adjusts text size to fit available space.
 *
 * Uses TextMeasurer and a bounded binary search to find the largest font size
 * that fits within the available width. This avoids size oscillation/animation
 * and produces a stable, centered result.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = TextAlign.Center,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minTextSize: TextUnit = 10.sp,
    maxTextSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val targetWidthPx = availableWidthPx * 0.96f // leave a small side margin

        // Determine bounds
        val initialFontSize = when {
            fontSize.isSpecified -> fontSize
            style.fontSize.isSpecified -> style.fontSize
            else -> 14.sp
        }
        val maxCandidate = if (maxTextSize.isSpecified) maxTextSize else initialFontSize
        // Use derivedStateOf to prevent animation when recalculating font size
        val computedFontSize by remember(text, availableWidthPx, initialFontSize, minTextSize, maxCandidate, fontStyle, fontWeight, fontFamily, letterSpacing) {
            derivedStateOf {
                var low = minTextSize.value
                var high = maxCandidate.value
                var best = low
                
                // Binary search for largest fitting size
                repeat(10) { // Reduced iterations for stability
                    val midSp = ((low + high) / 2f).sp
                    val candidateStyle = style.merge(
                        TextStyle(
                            fontSize = midSp,
                            fontStyle = fontStyle,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            letterSpacing = letterSpacing,
                            lineHeight = lineHeight
                        )
                    )
                    val result = measurer.measure(
                        text = text,
                        style = candidateStyle,
                        softWrap = false,
                        maxLines = 1,
                        overflow = overflow
                    )
                    val width = result.size.width.toFloat()
                    if (width <= targetWidthPx) {
                        best = midSp.value
                        low = midSp.value // try larger
                    } else {
                        high = midSp.value // too big
                    }
                }
                best.sp
            }
        }

        Text(
            text = text,
            color = color,
            textAlign = textAlign,
            softWrap = softWrap,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.fillMaxWidth(),
            style = style.merge(
                TextStyle(
                    fontSize = computedFontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    lineHeight = lineHeight
                )
            )
        )
    }
}
