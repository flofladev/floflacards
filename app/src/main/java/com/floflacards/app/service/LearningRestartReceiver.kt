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

package com.floflacards.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.floflacards.app.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Restarts the learning timer after the two events that silently kill it: a device
 * reboot (alarms and services do not survive one) and an app update (the process is
 * killed when the package is replaced). Without this, learning stays off until the
 * user notices and presses start again, while the saved state still says it is active.
 *
 * BOOT_COMPLETED is delivered after the first unlock, so preferences are readable and
 * no card can appear on a locked phone. Starting the foreground service from here is
 * permitted because the app holds SYSTEM_ALERT_WINDOW.
 */
@AndroidEntryPoint
class LearningRestartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LearningRestartReceiver"
    }

    @Inject
    lateinit var settingsManager: SettingsRepository

    @Inject
    lateinit var learningServiceManager: LearningServiceManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        // No demo overlay can have survived a reboot or an update; a stale "running"
        // flag would otherwise block every future timer start.
        settingsManager.setDemoRunning(false)

        if (!settingsManager.getIsLearningActive()) {
            Log.d(TAG, "Learning was not active, nothing to restart after $action")
            return
        }
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Learning was active but overlay permission is gone, not restarting")
            return
        }

        learningServiceManager.startLearningService(settingsManager.getIntervalMinutes())
        Log.d(TAG, "Learning timer restarted after $action")
    }
}
