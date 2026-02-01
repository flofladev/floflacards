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

import android.content.Context
import com.floflacards.app.service.TimerForegroundService
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.domain.manager.ServiceStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsRepository,
    private val serviceCommunicationManager: ServiceStateManager
) {
    val isServiceActive: StateFlow<Boolean> = serviceCommunicationManager.isServiceActive
    val countdownTime: StateFlow<Long> = serviceCommunicationManager.countdownTime
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        checkServiceState()
    }
    
    fun startLearningService(intervalMinutes: Int) {
        serviceCommunicationManager.updateServiceStatus(true)
        settingsManager.setLearningActive(true)
        TimerForegroundService.start(context, intervalMinutes)
    }
    
    fun stopLearningService() {
        serviceCommunicationManager.updateServiceStatus(false)
        serviceCommunicationManager.resetCountdown()
        settingsManager.setLearningActive(false)
        TimerForegroundService.stop(context)
    }
    

    
    private fun checkServiceState() {
        // Service state is now managed by ServiceStateManager
        // No additional logic needed here
    }
    
    fun cleanup() {
        serviceScope.cancel()
    }
}
