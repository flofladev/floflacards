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

package com.floflacards.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Single source of truth for every raw colour literal in the app.
 *
 * Rules of the road:
 *  - Components must read colours from [MaterialTheme.colorScheme] (assembled in Theme.kt from
 *    the tokens below) or from the semantic accessors at the bottom of this file.
 *  - No `Color(0xFF…)` literals anywhere else in the UI. If a new colour is needed, add a token
 *    here so light/dark stay in lockstep.
 *
 * The palette is a single cohesive cool family — violet (primary) → blue-violet (secondary) →
 * indigo (tertiary) — so the three "container" roles that drive most surfaces (Cards / Stats /
 * Settings, and the stat tiles) read as one system instead of the default Material maroon.
 */

// ---------------------------------------------------------------------------
// Light scheme tokens
// ---------------------------------------------------------------------------
val LightPrimary = Color(0xFF673AB7)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEADDFF)
val LightOnPrimaryContainer = Color(0xFF21005D)

val LightSecondary = Color(0xFF625B71)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE6DEF8)
val LightOnSecondaryContainer = Color(0xFF1D192B)

val LightTertiary = Color(0xFF7E5260)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF0DBFF)
val LightOnTertiaryContainer = Color(0xFF2B0052)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFFFFBFE)
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFFFBFE)
val LightOnSurface = Color(0xFF1C1B1F)
val LightSurfaceVariant = Color(0xFFE7E0EB)
val LightOnSurfaceVariant = Color(0xFF49454F)
val LightOutline = Color(0xFF7A757F)

// ---------------------------------------------------------------------------
// Dark scheme tokens
// ---------------------------------------------------------------------------
val DarkPrimary = Color(0xFF7E57C2)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF4F378B)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)

val DarkSecondary = Color(0xFFCCC2DC)
val DarkOnSecondary = Color(0xFF332D41)
val DarkSecondaryContainer = Color(0xFF463A6B)
val DarkOnSecondaryContainer = Color(0xFFE8DEF8)

val DarkTertiary = Color(0xFFD6BEE8)
val DarkOnTertiary = Color(0xFF3A2948)
val DarkTertiaryContainer = Color(0xFF573C7A)
val DarkOnTertiaryContainer = Color(0xFFF0DBFF)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackgroundColor = Color(0xFF121212)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkSurfaceColor = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkSurfaceVariantColor = Color(0xFF49454F)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val DarkOutline = Color(0xFF948F9A)

// ---------------------------------------------------------------------------
// Semantic "extended" colours
//
// Material 3 has no role for success / warning, but the learning controls genuinely need a
// "go" (start) and an "attention" (permission required) signal. They live here as tokens with
// explicit light/dark variants and are read through the accessors below so they honour the
// user's manual theme override, not just the system setting.
// ---------------------------------------------------------------------------
private val SuccessLight = Color(0xFF2E7D32)
private val SuccessDark = Color(0xFF4CAF50)

// "Stop" is a deliberate solid red in both themes (matching the solid green "start"), rather than
// the scheme's `error` role — Material's dark error is a pale red that reads weak on a filled button.
private val StopLight = Color(0xFFC62828)
private val StopDark = Color(0xFFE53935)

private val WarningLight = Color(0xFFEF6C00)
private val WarningDark = Color(0xFFF5A623)
private val WarningContainerLight = Color(0xFFFFE3C2)
private val OnWarningContainerLight = Color(0xFF5A3000)
private val WarningContainerDark = Color(0xFF4A2A00)
private val OnWarningContainerDark = Color(0xFFFFE3C2)

/**
 * True when the active scheme is dark. Derived from the resolved scheme luminance rather than
 * [isSystemInDarkTheme] so it stays correct when the user forces LIGHT/DARK from settings.
 */
@Composable
fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun successColor(): Color = if (isAppInDarkTheme()) SuccessDark else SuccessLight

@Composable
fun onSuccessColor(): Color = Color.White

@Composable
fun stopColor(): Color = if (isAppInDarkTheme()) StopDark else StopLight

@Composable
fun onStopColor(): Color = Color.White

@Composable
fun warningColor(): Color = if (isAppInDarkTheme()) WarningDark else WarningLight

@Composable
fun onWarningColor(): Color = Color.White

@Composable
fun warningContainerColor(): Color = if (isAppInDarkTheme()) WarningContainerDark else WarningContainerLight

@Composable
fun onWarningContainerColor(): Color = if (isAppInDarkTheme()) OnWarningContainerDark else OnWarningContainerLight
