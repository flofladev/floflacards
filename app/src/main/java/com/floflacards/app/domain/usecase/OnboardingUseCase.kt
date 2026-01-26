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
