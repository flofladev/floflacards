package com.floflacards.app.data.dao

import androidx.room.*
import com.floflacards.app.data.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    
    @Query("SELECT * FROM flashcards WHERE categoryId = :categoryId ORDER BY createdAt ASC")
    fun getFlashcardsByCategory(categoryId: Long): Flow<List<FlashcardEntity>>
    
    @Query("""
        SELECT f.* FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
        ORDER BY f.createdAt ASC
    """)
    suspend fun getAllFlashcards(): List<FlashcardEntity>
    
    @Query("""
        SELECT f.* FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        ORDER BY f.createdAt ASC
    """)
    suspend fun getAllFlashcardsForStatistics(): List<FlashcardEntity>
    
    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getFlashcardById(id: Long): FlashcardEntity?
    
    @Query("""
        SELECT f.* FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        WHERE f.isEnabled = 1 AND c.isEnabled = 1 
        AND f.cooldownUntil <= :currentTime
        ORDER BY 
            (f.cooldownUntil - f.lastReviewedAt) DESC,
            f.easinessFactor ASC,
            CASE WHEN (f.correctCount + f.incorrectCount) = 0 THEN 0.5
                 ELSE CAST(f.incorrectCount AS REAL) / (f.correctCount + f.incorrectCount) 
            END DESC,
            RANDOM()
        LIMIT 1
    """)
    suspend fun getNextFlashcardForReview(currentTime: Long = System.currentTimeMillis()): FlashcardEntity?
    
    @Query("""
        SELECT f.* FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
        ORDER BY 
            f.cooldownUntil ASC,
            f.easinessFactor ASC,
            CASE WHEN (f.correctCount + f.incorrectCount) = 0 THEN 0.5
                 ELSE CAST(f.incorrectCount AS REAL) / (f.correctCount + f.incorrectCount) 
            END DESC,
            RANDOM()
        LIMIT 1
    """)
    suspend fun getCardWithShortestCooldown(): FlashcardEntity?
    
    /**
     * Gets the next available flashcard, guaranteeing a result if any cards exist.
     * First tries to get a card that's ready for review, then falls back to the card
     * with the shortest remaining cooldown.
     */
    suspend fun getNextAvailableFlashcard(currentTime: Long = System.currentTimeMillis()): FlashcardEntity? {
        // First try to get a card that's ready for review
        getNextFlashcardForReview(currentTime)?.let { return it }
        
        // If no cards are ready, get the one with shortest cooldown
        return getCardWithShortestCooldown()
    }
    
    @Query("""
        SELECT COUNT(*) FROM flashcards f 
        INNER JOIN categories c ON f.categoryId = c.id 
        WHERE f.isEnabled = 1 AND c.isEnabled = 1
    """)
    suspend fun getActiveFlashcardCount(): Int
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE categoryId = :categoryId")
    suspend fun getFlashcardCountByCategory(categoryId: Long): Int
    
    @Insert
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long
    
    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)
    
    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)
    
    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Long)
    
    @Query("DELETE FROM flashcards WHERE categoryId = :categoryId")
    suspend fun deleteFlashcardsByCategoryId(categoryId: Long)
    
    // Statistics reset methods
    @Query("""
        UPDATE flashcards 
        SET correctCount = 0, 
            incorrectCount = 0, 
            hardCount = 0, 
            easinessFactor = 2.5, 
            reviewCount = 0, 
            lastReviewedAt = 0, 
            cooldownUntil = 0,
            updatedAt = :timestamp
        WHERE id = :flashcardId
    """)
    suspend fun resetFlashcardStatistics(flashcardId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE flashcards 
        SET correctCount = 0, 
            incorrectCount = 0, 
            hardCount = 0, 
            easinessFactor = 2.5, 
            reviewCount = 0, 
            lastReviewedAt = 0, 
            cooldownUntil = 0,
            updatedAt = :timestamp
        WHERE categoryId = :categoryId
    """)
    suspend fun resetCategoryStatistics(categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE flashcards 
        SET correctCount = 0, 
            incorrectCount = 0, 
            hardCount = 0, 
            easinessFactor = 2.5, 
            reviewCount = 0, 
            lastReviewedAt = 0, 
            cooldownUntil = 0,
            updatedAt = :timestamp
    """)
    suspend fun resetAllStatistics(timestamp: Long = System.currentTimeMillis())
    
    // Backup-specific methods
    @Query("SELECT * FROM flashcards WHERE question = :question AND answer = :answer LIMIT 1")
    suspend fun getFlashcardByQuestionAndAnswer(question: String, answer: String): FlashcardEntity?
    
    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards()
    
    // Bulk operations for select/deselect all functionality
    @Query("UPDATE flashcards SET isEnabled = 1, updatedAt = :timestamp WHERE categoryId = :categoryId")
    suspend fun enableAllFlashcardsInCategory(categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE flashcards SET isEnabled = 0, updatedAt = :timestamp WHERE categoryId = :categoryId")
    suspend fun disableAllFlashcardsInCategory(categoryId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE categoryId = :categoryId AND isEnabled = 1")
    suspend fun getEnabledFlashcardCountByCategory(categoryId: Long): Int
}
