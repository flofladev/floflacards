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
