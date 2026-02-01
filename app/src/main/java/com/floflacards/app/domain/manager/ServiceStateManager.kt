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
