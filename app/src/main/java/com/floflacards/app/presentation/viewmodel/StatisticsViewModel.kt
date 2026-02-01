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

package com.floflacards.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floflacards.app.data.repository.FlashcardRepository
import com.floflacards.app.domain.usecase.StatisticsUseCase
import com.floflacards.app.domain.usecase.SimpleStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardStats(
    val id: Long,
    val question: String,
    val answer: String,
    val correctCount: Int,
    val incorrectCount: Int,
    val hardCount: Int,
    val difficultyScore: Float,
    val successRate: Float,
    val lastSeenTimestamp: Long,
    val reviewCount: Int,
    val isEnabled: Boolean,
    val isMastered: Boolean
) {
    val lastSeenText: String = when {
        lastSeenTimestamp == 0L -> "Not reviewed yet"
        else -> {
            val date = java.util.Date(lastSeenTimestamp)
            val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            formatter.format(date)
        }
    }
    
    val difficultyLevel: String = when {
        difficultyScore <= 1.5f -> "Hard"
        difficultyScore <= 2.0f -> "Medium"
        else -> "Easy"
    }
    
    val totalAttempts: Int = correctCount + incorrectCount + hardCount // All attempts count for weighted success rate
}

data class CategoryStats(
    val categoryId: Long,
    val categoryName: String,
    val totalCards: Int,
    val studiedCards: Int,
    val masteredCards: Int,
    val averageSuccessRate: Float,
    val flashcards: List<FlashcardStats>,
    val isExpanded: Boolean = false
) {
    val studiedPercentage: Int = if (totalCards > 0) (studiedCards * 100) / totalCards else 0
    val masteredPercentage: Int = if (totalCards > 0) (masteredCards * 100) / totalCards else 0
    val masteredRate: Float = if (totalCards > 0) masteredCards.toFloat() / totalCards.toFloat() else 0f
}

data class EnhancedOverallStats(
    val streakDays: Int,
    val highestStreak: Int,
    val masteredFlashcards: Int,
    val totalFlashcards: Int
)

data class ModernStatisticsUiState(
    val isLoading: Boolean = false,
    val overallStats: EnhancedOverallStats? = null,
    val categoryStats: List<CategoryStats> = emptyList(),
    val searchQuery: String = "" // Current search query
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsUseCase: StatisticsUseCase,
    private val repository: FlashcardRepository,
    private val simpleStreakUseCase: SimpleStreakUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ModernStatisticsUiState())
    val uiState: StateFlow<ModernStatisticsUiState> = _uiState.asStateFlow()
    
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val allFlashcards = repository.getAllFlashcardsForStatistics()
                val allCategories = repository.getAllCategories()
                
                // Calculate enhanced overall stats using new streak system
                val masteredFlashcards = allFlashcards.count { it.easinessFactor >= 2.5f && it.reviewCount >= 3 }
                val totalFlashcards = allFlashcards.size
                
                // Use new simple streak system instead of complex historical calculation
                val currentStreakData = simpleStreakUseCase.getCurrentStreakData()
                val streakDays = currentStreakData.currentStreak
                val highestStreak = currentStreakData.highestStreak
                
                val enhancedOverallStats = EnhancedOverallStats(
                    streakDays = streakDays,
                    highestStreak = highestStreak,
                    masteredFlashcards = masteredFlashcards,
                    totalFlashcards = totalFlashcards
                )
                
                allCategories.collect { categories ->
                    val categoryStatsList = categories
                        .sortedBy { it.createdAt } // Sort categories by creation date
                        .map { category ->
                            val categoryFlashcards = allFlashcards.filter { it.categoryId == category.id }
                            val studiedCards = categoryFlashcards.count { it.reviewCount > 0 }
                            val masteredCards = categoryFlashcards.count { it.easinessFactor >= 2.5f && it.reviewCount >= 3 }
                            
                            val flashcardStats = categoryFlashcards.map { flashcard ->
                                // Weighted success rate: Good=1.0, Hard=0.5, Wrong=0.0
                                val totalAttempts = flashcard.correctCount + flashcard.incorrectCount + flashcard.hardCount
                                val successRate = if (totalAttempts > 0) {
                                    val weightedScore = (flashcard.correctCount * 1.0f) + (flashcard.hardCount * 0.5f)
                                    (weightedScore / totalAttempts.toFloat()) * 100f
                                } else 0f
                                
                                val isMastered = flashcard.easinessFactor >= 2.5f && flashcard.reviewCount >= 3
                                
                                FlashcardStats(
                                    id = flashcard.id,
                                    question = flashcard.question,
                                    answer = flashcard.answer,
                                    correctCount = flashcard.correctCount,
                                    incorrectCount = flashcard.incorrectCount,
                                    hardCount = flashcard.hardCount,
                                    difficultyScore = flashcard.easinessFactor,
                                    successRate = successRate,
                                    lastSeenTimestamp = flashcard.lastReviewedAt,
                                    reviewCount = flashcard.reviewCount,
                                    isEnabled = flashcard.isEnabled,
                                    isMastered = isMastered
                                )
                            }.sortedWith(
                                compareByDescending<FlashcardStats> { it.successRate } // Best success rate first
                                    .thenByDescending { it.reviewCount } // Most reviewed first
                                    .thenBy { if (it.lastSeenTimestamp == 0L) Long.MAX_VALUE else -it.lastSeenTimestamp } // Never seen at bottom
                            )
                        
                        val averageSuccessRate = if (categoryFlashcards.isNotEmpty()) {
                            categoryFlashcards.map { flashcard ->
                                val totalAttempts = flashcard.correctCount + flashcard.incorrectCount + flashcard.hardCount
                                if (totalAttempts > 0) {
                                    val weightedScore = (flashcard.correctCount * 1.0f) + (flashcard.hardCount * 0.5f)
                                    weightedScore / totalAttempts.toFloat()
                                } else 0f
                            }.average().toFloat()
                        } else 0f
                        
                        CategoryStats(
                            categoryId = category.id,
                            categoryName = category.name,
                            totalCards = categoryFlashcards.size,
                            studiedCards = studiedCards,
                            masteredCards = masteredCards,
                            averageSuccessRate = averageSuccessRate,
                            flashcards = flashcardStats
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        overallStats = enhancedOverallStats,
                        categoryStats = categoryStatsList
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                println("Failed to load statistics: ${e.message}")
            }
        }
    }
    
    fun toggleCategoryExpansion(categoryId: Long) {
        val currentStats = _uiState.value.categoryStats
        val updatedStats = currentStats.map { category ->
            if (category.categoryId == categoryId) {
                category.copy(isExpanded = !category.isExpanded)
            } else {
                category
            }
        }
        _uiState.value = _uiState.value.copy(categoryStats = updatedStats)
    }
    
    // Search functionality
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query.trim())
    }
    
    /**
     * Returns filtered category stats based on search query.
     * Searches both category names and flashcard content (questions/answers).
     * Categories are included if:
     * - The category name matches the query, OR
     * - Any flashcard within the category matches the query
     */
    fun getFilteredCategoryStats(): List<CategoryStats> {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) {
            return _uiState.value.categoryStats
        }
        
        return _uiState.value.categoryStats.mapNotNull { category ->
            val categoryNameMatches = category.categoryName.contains(query, ignoreCase = true)
            
            // Filter flashcards within the category
            val matchingFlashcards = category.flashcards.filter { flashcard ->
                flashcard.question.contains(query, ignoreCase = true) ||
                flashcard.answer.contains(query, ignoreCase = true)
            }
            
            when {
                // Category name matches - include with all flashcards
                categoryNameMatches -> category
                // Some flashcards match - include category with only matching flashcards
                matchingFlashcards.isNotEmpty() -> category.copy(
                    flashcards = matchingFlashcards,
                    isExpanded = true // Auto-expand to show matched flashcards
                )
                // No match - exclude category
                else -> null
            }
        }
    }
    
    // Reset statistics methods
    fun resetFlashcardStatistics(flashcardId: Long) {
        viewModelScope.launch {
            try {
                repository.resetFlashcardStatistics(flashcardId)
                // Reload statistics to reflect changes
                loadStatistics()
            } catch (e: Exception) {
                println("Failed to reset flashcard statistics: ${e.message}")
            }
        }
    }
    
    fun resetCategoryStatistics(categoryId: Long) {
        viewModelScope.launch {
            try {
                repository.resetCategoryStatistics(categoryId)
                // Reload statistics to reflect changes
                loadStatistics()
            } catch (e: Exception) {
                println("Failed to reset category statistics: ${e.message}")
            }
        }
    }
    
    fun resetAllStatistics() {
        viewModelScope.launch {
            try {
                repository.resetAllStatistics()
                // Also reset streak data when resetting all statistics
                simpleStreakUseCase.resetStreak()
                // Reload statistics to reflect changes
                loadStatistics()
            } catch (e: Exception) {
                println("Failed to reset all statistics: ${e.message}")
            }
        }
    }
}
