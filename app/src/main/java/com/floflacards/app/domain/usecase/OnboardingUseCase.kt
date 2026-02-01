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

package com.floflacards.app.domain.usecase

import com.floflacards.app.data.repository.FlashcardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingUseCase @Inject constructor(
    private val repository: FlashcardRepository
) {
    
    /**
     * Creates sample data for first-time users.
     * Tutorial content is now handled by the Welcome Screen.
     * This method is kept for potential future sample data needs.
     */
    suspend fun createSampleDataIfNeeded() {
        // Tutorial category removed - now handled by Welcome Screen
        // Users will learn through the mandatory onboarding flow
        // No sample data needed as users will create their own flashcards
        
        // This method can be extended in the future if we want to add
        // sample categories for specific subjects (e.g., "Sample Math", "Sample Language")
    }
}
