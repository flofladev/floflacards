package com.floflacards.app.domain.usecase

import com.floflacards.app.data.repository.FlashcardRepository
import javax.inject.Inject
import javax.inject.Singleton

data class SimpleStatistics(
    val totalCards: Int,
    val studiedCards: Int,
    val accuracyRate: Float,
    val streakDays: Int
) {
    val studiedPercentage: Int = if (totalCards > 0) (studiedCards * 100) / totalCards else 0
}

// StreakCalculator object removed - replaced with SimpleStreakUseCase for better UX
// Old complex historical calculation replaced with simple, predictable streak tracking

@Singleton
class StatisticsUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val simpleStreakUseCase: SimpleStreakUseCase
) {
    
    suspend fun getSimpleStatistics(): Result<SimpleStatistics> {
        return try {
            // Get enabled flashcards for regular statistics (total cards, studied cards, etc.)
            val enabledFlashcards = repository.getAllFlashcards()
            
            val totalCards = enabledFlashcards.size
            val studiedCards = enabledFlashcards.count { it.reviewCount > 0 }
            
            val totalCorrect = enabledFlashcards.sumOf { it.correctCount }
            val totalIncorrect = enabledFlashcards.sumOf { it.incorrectCount }
            val totalAttempts = totalCorrect + totalIncorrect
            val accuracyRate = if (totalAttempts > 0) {
                totalCorrect.toFloat() / totalAttempts.toFloat()
            } else 0f
            
            // Use new simple streak system instead of complex historical calculation
            val currentStreakData = simpleStreakUseCase.getCurrentStreakData()
            val streakDays = currentStreakData.currentStreak
            
            val stats = SimpleStatistics(
                totalCards = totalCards,
                studiedCards = studiedCards,
                accuracyRate = accuracyRate,
                streakDays = streakDays
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
