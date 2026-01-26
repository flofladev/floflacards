package com.floflacards.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.Language
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FloatingLearningApplication : Application() {
    
    @Inject
    lateinit var settingsManager: SettingsRepository
    
    override fun onCreate() {
        super.onCreate()
        initializeLocale()
    }
    
    /**
     * Initializes the app locale based on user preferences.
     * Applies the saved locale immediately on app startup.
     */
    private fun initializeLocale() {
        val savedLanguage = settingsManager.getAppLocale()
        val localeTag = Language.toLanguageTag(savedLanguage.code)
        val localeList = if (localeTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList() // Use system default
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
