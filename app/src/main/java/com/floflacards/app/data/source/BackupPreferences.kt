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

package com.floflacards.app.data.source

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages backup-related preferences to track first launch and backup dialog state.
 * Follows single responsibility principle.
 */
@Singleton
class BackupPreferences @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "backup_preferences"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_BACKUP_DIALOG_SHOWN = "backup_dialog_shown"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
        private const val KEY_APP_INSTALLED_TIMESTAMP = "app_installed_timestamp"
        private const val KEY_SAF_TREE_URI = "saf_tree_uri"
        
        // Session tracking to prevent multiple checks in same app session
        private var hasCheckedThisSession = false
    }

    /**
     * Checks if this is the first app launch.
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Marks that the first launch has been completed.
     */
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * Checks if backup dialog has been shown for the current backup file.
     */
    fun hasBackupDialogBeenShown(backupTimestamp: Long): Boolean {
        val lastShownTimestamp = prefs.getLong(KEY_BACKUP_DIALOG_SHOWN, 0L)
        return lastShownTimestamp >= backupTimestamp
    }

    /**
     * Marks that backup dialog has been shown for the current backup.
     */
    fun setBackupDialogShown(backupTimestamp: Long) {
        prefs.edit().putLong(KEY_BACKUP_DIALOG_SHOWN, backupTimestamp).apply()
    }

    /**
     * Gets the timestamp when app was first installed.
     */
    fun getAppInstalledTimestamp(): Long {
        val timestamp = prefs.getLong(KEY_APP_INSTALLED_TIMESTAMP, 0L)
        if (timestamp == 0L) {
            // First time accessing this, set current time
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_APP_INSTALLED_TIMESTAMP, currentTime).apply()
            return currentTime
        }
        return timestamp
    }

    /**
     * Checks if backup has been checked in this app session.
     */
    fun hasCheckedThisSession(): Boolean {
        return hasCheckedThisSession
    }
    
    /**
     * Marks that backup has been checked in this session.
     */
    fun setCheckedThisSession() {
        hasCheckedThisSession = true
    }

    /**
     * Determines if backup restore dialog should be shown.
     * Logic: Show dialog if:
     * 1. Haven't checked this session yet AND
     * 2. It's first launch AND backup exists
     * 3. Backup file is newer than last shown dialog
     * 4. App was reinstalled (no local data but backup exists)
     */
    fun shouldShowBackupDialog(
        backupExists: Boolean,
        backupTimestamp: Long,
        hasLocalData: Boolean
    ): Boolean {
        // Don't show if already checked this session
        if (hasCheckedThisSession) return false
        
        if (!backupExists) return false

        val isFirstLaunch = isFirstLaunch()
        val dialogAlreadyShown = hasBackupDialogBeenShown(backupTimestamp)
        val appInstalledTime = getAppInstalledTimestamp()

        return when {
            // First launch with existing backup - always show
            isFirstLaunch && backupExists -> true
            
            // App reinstalled (no local data but backup exists and is newer than app install)
            !hasLocalData && backupExists && backupTimestamp > appInstalledTime -> true
            
            // New backup file that hasn't been handled yet
            backupExists && !dialogAlreadyShown -> true
            
            // All other cases - don't show
            else -> false
        }
    }

    /**
     * Gets the SAF tree URI for backup folder.
     */
    fun getSafTreeUri(): String? {
        return prefs.getString(KEY_SAF_TREE_URI, null)
    }
    
    /**
     * Sets the SAF tree URI for backup folder.
     */
    fun setSafTreeUri(uri: String?) {
        prefs.edit().putString(KEY_SAF_TREE_URI, uri).apply()
    }
    
    /**
     * Checks if SAF folder is configured.
     */
    fun hasSafFolderConfigured(): Boolean {
        return getSafTreeUri() != null
    }

    /**
     * Resets backup dialog state (for testing purposes).
     */
    fun resetBackupDialogState() {
        prefs.edit()
            .remove(KEY_BACKUP_DIALOG_SHOWN)
            .remove(KEY_FIRST_LAUNCH)
            .apply()
    }
}
