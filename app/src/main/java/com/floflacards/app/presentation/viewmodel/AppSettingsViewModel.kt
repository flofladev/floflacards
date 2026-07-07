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

package com.floflacards.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.AppTheme
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.data.model.Language
import com.floflacards.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for AppSettingsScreen following MVVM architecture.
 * Handles theme preference management and other app settings.
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Manages app settings state
 * - Dependency Inversion: Depends on SettingsRepository abstraction
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val settingsManager: SettingsRepository,
    private val permissionHelper: PermissionHelper
) : ViewModel() {
    
    /**
     * Current app theme preference as StateFlow
     * CRITICAL: This controls app theme independently from device theme
     */
    val appTheme: StateFlow<AppTheme> = settingsManager.appTheme
    
    /**
     * Current flashcard theme preference as StateFlow
     * CRITICAL: This controls flashcard theme independently from both app and device theme
     */
    val flashcardTheme: StateFlow<FlashcardTheme> = settingsManager.flashcardTheme
    
    /**
     * Current app locale preference as StateFlow
     * CRITICAL: This controls app language independently from system locale
     */
    val appLocale: StateFlow<Language> = settingsManager.appLocale

    /**
     * Whether the floating card hides itself while a soft keyboard is on screen.
     */
    val hideCardWhileTyping: StateFlow<Boolean> = settingsManager.hideCardWhileTyping

    /**
     * Whether the floating card hides itself while the screen is in landscape
     * (a proxy for games and fullscreen video).
     */
    val hideCardInLandscape: StateFlow<Boolean> = settingsManager.hideCardInLandscape

    /**
     * Updates the app theme preference
     * CRITICAL: This will immediately change the app theme
     */
    fun setAppTheme(theme: AppTheme) {
        settingsManager.setAppTheme(theme)
    }

    /**
     * Toggles whether the floating card hides while a keyboard is on screen.
     * Takes effect on the next card shown.
     */
    fun setHideCardWhileTyping(enabled: Boolean) {
        settingsManager.setHideCardWhileTyping(enabled)
    }

    /**
     * Toggles whether the floating card hides in landscape.
     * Takes effect on the next card shown.
     */
    fun setHideCardInLandscape(enabled: Boolean) {
        settingsManager.setHideCardInLandscape(enabled)
    }
    
    /**
     * Updates the flashcard theme preference
     * CRITICAL: This will immediately change the flashcard theme
     */
    fun setFlashcardTheme(theme: FlashcardTheme) {
        settingsManager.setFlashcardTheme(theme)
    }
    
    /**
     * Updates the app locale preference
     * CRITICAL: This will immediately change the app language
     */
    fun setAppLocale(language: Language) {
        settingsManager.setAppLocale(language)
    }
    
    /**
     * Checks if battery optimization is disabled.
     * Follows SRP by delegating to PermissionHelper.
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        return permissionHelper.isBatteryOptimizationDisabled()
    }
    
    /**
     * Checks if user has skipped battery optimization during welcome flow.
     * Follows SRP by delegating to SettingsRepository.
     */
    fun isBatteryOptimizationSkipped(): Boolean {
        return settingsManager.isBatteryOptimizationSkipped()
    }
    
    /**
     * Requests battery optimization disable.
     * Follows SRP by delegating to PermissionHelper.
     */
    fun requestBatteryOptimizationDisable() {
        permissionHelper.requestBatteryOptimizationDisable()
    }
}
