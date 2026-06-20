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
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.floflacards.app.data.backup.BackupScheduler
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.model.Language
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FloatingLearningApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var settingsManager: SettingsRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backupScheduler: BackupScheduler

    // Wires Hilt-injected workers (e.g. BackupWorker) into WorkManager.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initializeLocale()
        scheduleAutomaticBackups()
    }

    /**
     * Sets up the deferred backup system: a daily periodic backup plus a
     * one-shot backup whenever the app moves to the background (end of a
     * usage session). Both are dirty-checked, so they no-op when nothing
     * has changed. Backups are no longer written on every database edit.
     */
    private fun scheduleAutomaticBackups() {
        backupScheduler.schedulePeriodicBackup()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App went to the background — capture the session.
                backupScheduler.requestBackupNow()
            }
        })
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
