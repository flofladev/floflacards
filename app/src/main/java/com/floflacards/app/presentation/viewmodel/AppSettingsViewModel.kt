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
     * Updates the app theme preference
     * CRITICAL: This will immediately change the app theme
     */
    fun setAppTheme(theme: AppTheme) {
        settingsManager.setAppTheme(theme)
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
