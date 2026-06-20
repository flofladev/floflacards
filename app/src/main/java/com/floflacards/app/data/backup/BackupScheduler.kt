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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues automatic backups via WorkManager.
 *
 * Two triggers, both funnelled through [BackupWorker] (which dirty-checks
 * internally, so an enqueue with no pending changes is essentially free):
 *  - a daily periodic backup for steady disaster recovery, and
 *  - a one-shot backup when the app goes to the background.
 *
 * Replaces the old behaviour of writing the full backup file synchronously
 * after every single database edit.
 */
@Singleton
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PERIODIC_WORK_NAME = "floflacards_periodic_backup"
        private const val ONE_TIME_WORK_NAME = "floflacards_background_backup"
        private const val PERIODIC_INTERVAL_HOURS = 24L
        private const val BACKOFF_MINUTES = 10L
    }

    private val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    /**
     * Schedules the daily periodic backup. Safe to call on every app start —
     * KEEP policy means an already-scheduled job is left untouched.
     */
    fun schedulePeriodicBackup() {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Enqueues a one-shot backup, used when the app goes to the background.
     * REPLACE policy coalesces rapid foreground/background toggles into a
     * single pending job.
     */
    fun requestBackupNow() {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
