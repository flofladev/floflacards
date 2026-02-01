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
