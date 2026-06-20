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

package com.floflacards.app.data.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.domain.usecase.backup.CreateBackupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Performs an automatic backup in the background.
 *
 * This worker is the single automatic backup path. It is enqueued:
 *  - periodically (once a day) for steady disaster-recovery snapshots, and
 *  - once when the app goes to the background (end of a usage session).
 *
 * It is intentionally cheap when nothing changed: it first checks the
 * dirty flag and returns immediately if the data is identical to the last
 * successful backup, so users syncing the backup folder (Syncthing/Nextcloud)
 * don't get a rewritten file when there's nothing new to save.
 *
 * The manual "Create backup" button does NOT go through this worker — it calls
 * the use case directly and always writes.
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val createBackupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferences,
    private val categoryDao: CategoryDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "BackupWorker"
    }

    override suspend fun doWork(): Result {
        // Nothing to back up to if no folder is configured.
        if (!backupPreferences.hasSafFolderConfigured()) {
            Log.d(TAG, "No backup folder configured, skipping")
            return Result.success()
        }

        // CRITICAL: never overwrite an existing backup with an empty database.
        // A fresh install (or any transient empty state) must not clobber a real
        // backup before the user has had the chance to restore it. Flashcards
        // can't exist without a category, so zero categories means an empty DB.
        if (categoryDao.getCategoryCount() == 0) {
            Log.d(TAG, "Database is empty, skipping automatic backup to protect existing backup")
            return Result.success()
        }

        // Skip if data hasn't changed since the last successful backup.
        if (!backupPreferences.hasUnbackedUpChanges()) {
            Log.d(TAG, "No changes since last backup, skipping")
            return Result.success()
        }

        // Capture the version BEFORE the backup so concurrent edits during the
        // write aren't incorrectly marked as already-backed-up.
        val versionAtStart = backupPreferences.getDataVersion()

        return createBackupUseCase().fold(
            onSuccess = {
                backupPreferences.setLastBackedUpVersion(versionAtStart)
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())
                Log.d(TAG, "Automatic backup completed (version=$versionAtStart)")
                Result.success()
            },
            onFailure = { error ->
                Log.w(TAG, "Automatic backup failed: ${error.message}")
                // Retry later; do not advance the backed-up version.
                Result.retry()
            }
        )
    }
}
