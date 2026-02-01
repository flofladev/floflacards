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

package com.floflacards.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.floflacards.app.data.model.AppTheme
import com.floflacards.app.data.model.FlashcardTheme
import com.floflacards.app.data.model.Language
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "floating_learning_settings",
        Context.MODE_PRIVATE
    )
    
    private val _intervalMinutes = MutableStateFlow(getIntervalMinutes())
    val intervalMinutes: StateFlow<Int> = _intervalMinutes.asStateFlow()
    
    // Learning state tracking to prevent OverlayService from restarting timer when stopped
    private val _isLearningActive = MutableStateFlow(getIsLearningActive())
    val isLearningActive: StateFlow<Boolean> = _isLearningActive.asStateFlow()
    
    // Theme preference tracking - app theme is independent from device theme
    private val _appTheme = MutableStateFlow(getAppTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()
    
    // Flashcard theme preference tracking - independent from both app and device theme
    private val _flashcardTheme = MutableStateFlow(getFlashcardTheme())
    val flashcardTheme: StateFlow<FlashcardTheme> = _flashcardTheme.asStateFlow()
    
    // App locale preference tracking - allows user to override system locale
    private val _appLocale = MutableStateFlow(getAppLocale())
    val appLocale: StateFlow<Language> = _appLocale.asStateFlow()
    
    companion object {
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_IS_LEARNING_ACTIVE = "is_learning_active"
        private const val KEY_HAS_SHOWN_FIRST_DEMO = "has_shown_first_demo"
        private const val KEY_IS_DEMO_RUNNING = "is_demo_running"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_FLASHCARD_THEME = "flashcard_theme"
        private const val KEY_BATTERY_OPTIMIZATION_SKIPPED = "battery_optimization_skipped"
        private const val KEY_BATTERY_OPTIMIZATION_EVER_DISABLED = "battery_optimization_ever_disabled"
        private const val KEY_APP_LOCALE = "app_locale"
        private const val DEFAULT_INTERVAL_MINUTES = 5
    }
    
    fun getIntervalMinutes(): Int {
        return prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)
    }
    
    fun setIntervalMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_INTERVAL_MINUTES, minutes)
            .apply()
        _intervalMinutes.value = minutes
    }
    
    fun getIsLearningActive(): Boolean {
        return prefs.getBoolean(KEY_IS_LEARNING_ACTIVE, false)
    }
    
    fun setLearningActive(isActive: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_LEARNING_ACTIVE, isActive)
            .apply()
        _isLearningActive.value = isActive
    }
    
    /**
     * Checks if the first-time demo has been shown.
     * Follows SRP by handling only demo state tracking.
     */
    fun hasShownFirstDemo(): Boolean {
        return prefs.getBoolean(KEY_HAS_SHOWN_FIRST_DEMO, false)
    }
    
    /**
     * Marks the first-time demo as shown.
     * Follows SRP by handling only demo state persistence.
     */
    fun setFirstDemoShown() {
        prefs.edit()
            .putBoolean(KEY_HAS_SHOWN_FIRST_DEMO, true)
            .apply()
    }
    
    /**
     * Checks if demo is currently running.
     * Used to prevent timer conflicts during demo.
     */
    fun isDemoRunning(): Boolean {
        return prefs.getBoolean(KEY_IS_DEMO_RUNNING, false)
    }
    
    /**
     * Sets the demo running state.
     * Used to isolate demo from timer system.
     */
    fun setDemoRunning(isRunning: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_DEMO_RUNNING, isRunning)
            .apply()
    }
    
    /**
     * Gets the current app theme preference.
     * CRITICAL: App theme is independent from device theme.
     * Follows SRP by handling only theme preference retrieval.
     */
    fun getAppTheme(): AppTheme {
        val themeString = prefs.getString(KEY_APP_THEME, AppTheme.DEFAULT.name)
        return AppTheme.fromString(themeString ?: AppTheme.DEFAULT.name)
    }
    
    /**
     * Sets the app theme preference.
     * CRITICAL: This controls the app theme independently from device theme.
     * Follows SRP by handling only theme preference persistence.
     */
    fun setAppTheme(theme: AppTheme) {
        prefs.edit()
            .putString(KEY_APP_THEME, theme.name)
            .apply()
        _appTheme.value = theme
    }
    
    /**
     * Checks if user has skipped battery optimization during welcome flow.
     * Follows SRP by handling only battery optimization skip state retrieval.
     */
    fun isBatteryOptimizationSkipped(): Boolean {
        return prefs.getBoolean(KEY_BATTERY_OPTIMIZATION_SKIPPED, false)
    }
    
    /**
     * Sets the battery optimization skip preference.
     * Used when user chooses to skip battery optimization in welcome screen.
     * Follows SRP by handling only battery optimization skip state persistence.
     */
    fun setBatteryOptimizationSkipped(isSkipped: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BATTERY_OPTIMIZATION_SKIPPED, isSkipped)
            .apply()
    }
    
    /**
     * Checks if user has ever successfully disabled battery optimization.
     * Different from skip - indicates user wants the feature but may need help if re-enabled.
     * Follows SRP by handling only battery optimization disable tracking.
     */
    fun hasBatteryOptimizationEverBeenDisabled(): Boolean {
        return prefs.getBoolean(KEY_BATTERY_OPTIMIZATION_EVER_DISABLED, false)
    }
    
    /**
     * Marks that user has successfully disabled battery optimization.
     * Used to track user intent for contextual help vs welcome screen.
     * Follows SRP by handling only battery optimization disable state persistence.
     */
    fun setBatteryOptimizationEverDisabled(everDisabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BATTERY_OPTIMIZATION_EVER_DISABLED, everDisabled)
            .apply()
    }
    
    /**
     * Gets the current flashcard theme preference.
     * CRITICAL: Flashcard theme is independent from both app theme and device theme.
     * Follows SRP by handling only flashcard theme preference retrieval.
     */
    fun getFlashcardTheme(): FlashcardTheme {
        val themeString = prefs.getString(KEY_FLASHCARD_THEME, FlashcardTheme.DEFAULT_THEME.name)
        return FlashcardTheme.fromString(themeString ?: FlashcardTheme.DEFAULT_THEME.name)
    }
    
    /**
     * Sets the flashcard theme preference.
     * CRITICAL: This controls the flashcard theme independently from app and device theme.
     * Follows SRP by handling only flashcard theme preference persistence.
     */
    fun setFlashcardTheme(theme: FlashcardTheme) {
        prefs.edit()
            .putString(KEY_FLASHCARD_THEME, theme.name)
            .apply()
        _flashcardTheme.value = theme
    }
    
    /**
     * Gets the current app locale preference.
     * Returns SYSTEM by default to respect user's device locale.
     * Follows SRP by handling only locale preference retrieval.
     */
    fun getAppLocale(): Language {
        val localeCode = prefs.getString(KEY_APP_LOCALE, Language.SYSTEM.code)
        return Language.fromCode(localeCode ?: Language.SYSTEM.code)
    }
    
    /**
     * Sets the app locale preference and applies it immediately.
     * CRITICAL: This overrides the system locale for the entire app.
     * Follows SRP by handling only locale preference persistence and application.
     */
    fun setAppLocale(language: Language) {
        prefs.edit()
            .putString(KEY_APP_LOCALE, language.code)
            .apply()
        _appLocale.value = language
        
        // Apply locale immediately using AppCompatDelegate
        val localeTag = Language.toLanguageTag(language.code)
        val localeList = if (localeTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList() // Use system default
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
