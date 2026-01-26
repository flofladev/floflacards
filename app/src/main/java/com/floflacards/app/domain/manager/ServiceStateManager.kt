package com.floflacards.app.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages service state using StateFlow for reactive communication
 * Tracks countdown time and service active status
 * Follows SOLID principles and provides type-safe communication
 */
@Singleton
class ServiceStateManager @Inject constructor() {
    
    private val _countdownTime = MutableStateFlow(0L)
    val countdownTime: StateFlow<Long> = _countdownTime.asStateFlow()
    
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()
    
    fun updateCountdownTime(time: Long) {
        _countdownTime.value = time
    }
    
    fun updateServiceStatus(isActive: Boolean) {
        _isServiceActive.value = isActive
    }
    
    fun resetCountdown() {
        _countdownTime.value = 0L
    }
}
