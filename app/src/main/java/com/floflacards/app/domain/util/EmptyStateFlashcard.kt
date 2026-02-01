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

package com.floflacards.app.domain.util

import com.floflacards.app.data.entity.FlashcardEntity

/**
 * Utility for creating empty state flashcards when no cards are available.
 * Follows SOLID principles by separating empty state logic from repository.
 * Uses reserved ID to distinguish from regular flashcards and demo cards.
 */
object EmptyStateFlashcard {
    
    /**
     * Reserved ID for empty state flashcard.
     * Uses -2L to avoid conflict with demo flashcard (-1L) and regular flashcards (>0L).
     */
    const val EMPTY_STATE_ID = -2L
    
    /**
     * Creates an empty state flashcard with user-friendly messaging.
     * Reuses existing FlashcardEntity structure for compatibility.
     * 
     * @return FlashcardEntity configured as empty state indicator
     */
    fun create(): FlashcardEntity {
        return FlashcardEntity(
            id = EMPTY_STATE_ID,
            categoryId = 0L, // No category association
            question = "No Cards Available", // This will be overridden by UI components
            answer = "All your flashcards are currently disabled.\n\nTo continue learning:\n• Enable some flashcards in your categories\n• Or add new flashcards to study", // This will be overridden by UI components
            isEnabled = true, // Always enabled for display
            // Default values for other fields - not used in empty state
            correctCount = 0,
            incorrectCount = 0,
            hardCount = 0,
            easinessFactor = 2.5f,
            reviewCount = 0,
            lastReviewedAt = 0L,
            cooldownUntil = 0L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Checks if a flashcard is an empty state flashcard.
     * 
     * @param flashcard The flashcard to check
     * @return true if this is an empty state flashcard
     */
    fun isEmptyState(flashcard: FlashcardEntity): Boolean {
        return flashcard.id == EMPTY_STATE_ID
    }
    
    /**
     * Checks if a flashcard is a special system flashcard (demo or empty state).
     * 
     * @param flashcard The flashcard to check
     * @return true if this is a system flashcard (not a regular user flashcard)
     */
    fun isSystemFlashcard(flashcard: FlashcardEntity): Boolean {
        return flashcard.id < 0L // Negative IDs are reserved for system cards
    }
}
