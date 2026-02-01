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

package com.floflacards.app.data.repository

import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.domain.usecase.backup.CreateBackupUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val flashcardDao: FlashcardDao,
    private val createBackupUseCase: CreateBackupUseCase
) {
    
    // Category operations
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    
    fun getEnabledCategories(): Flow<List<CategoryEntity>> = categoryDao.getEnabledCategories()
    
    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)
    
    suspend fun insertCategory(category: CategoryEntity): Long {
        val result = categoryDao.insertCategory(category)
        // Create backup after category creation
        createBackupUseCase()
        return result
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
        // Create backup after category update
        createBackupUseCase()
    }
    
    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
        // Create backup after category deletion
        createBackupUseCase()
    }
    
    suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()
    
    // Flashcard operations
    fun getFlashcardsByCategory(categoryId: Long): Flow<List<FlashcardEntity>> = 
        flashcardDao.getFlashcardsByCategory(categoryId)
    
    suspend fun getAllFlashcards(): List<FlashcardEntity> = flashcardDao.getAllFlashcards()
    
    suspend fun getAllFlashcardsForStatistics(): List<FlashcardEntity> = flashcardDao.getAllFlashcardsForStatistics()
    
    suspend fun getFlashcardById(id: Long): FlashcardEntity? = flashcardDao.getFlashcardById(id)
    
    suspend fun getNextFlashcardForReview(): FlashcardEntity? = flashcardDao.getNextFlashcardForReview()
    
    /**
     * Gets the next available flashcard with guaranteed result.
     * Returns empty state flashcard when no cards are available instead of null.
     * This ensures the timer service never gets stuck and provides clear user feedback.
     */
    suspend fun getNextAvailableFlashcard(): FlashcardEntity {
        // Try to get a regular flashcard first
        val regularFlashcard = flashcardDao.getNextAvailableFlashcard()
        
        // If no regular cards available, return empty state flashcard
        return regularFlashcard ?: com.floflacards.app.domain.util.EmptyStateFlashcard.create()
    }
    
    suspend fun getCardWithShortestCooldown(): FlashcardEntity? = flashcardDao.getCardWithShortestCooldown()
    
    suspend fun getActiveFlashcardCount(): Int = flashcardDao.getActiveFlashcardCount()
    
    suspend fun getFlashcardCountByCategory(categoryId: Long): Int = flashcardDao.getFlashcardCountByCategory(categoryId)
    
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long {
        val result = flashcardDao.insertFlashcard(flashcard)
        // Create backup after flashcard creation
        createBackupUseCase()
        return result
    }
    
    suspend fun updateFlashcard(flashcard: FlashcardEntity) {
        flashcardDao.updateFlashcard(flashcard)
        // Create backup after flashcard update
        createBackupUseCase()
    }
    
    suspend fun deleteFlashcard(flashcard: FlashcardEntity) {
        flashcardDao.deleteFlashcard(flashcard)
        // Create backup after flashcard deletion
        createBackupUseCase()
    }
    
    // Statistics reset operations
    suspend fun resetFlashcardStatistics(flashcardId: Long) {
        flashcardDao.resetFlashcardStatistics(flashcardId)
        // Create backup after statistics reset
        createBackupUseCase()
    }
    
    suspend fun resetCategoryStatistics(categoryId: Long) {
        flashcardDao.resetCategoryStatistics(categoryId)
        // Create backup after statistics reset
        createBackupUseCase()
    }
    
    suspend fun resetAllStatistics() {
        flashcardDao.resetAllStatistics()
        // Create backup after statistics reset
        createBackupUseCase()
    }
    
    // Bulk operations for select/deselect all functionality
    suspend fun enableAllCategories() {
        categoryDao.enableAllCategories()
        // Create backup after bulk category operation
        createBackupUseCase()
    }
    
    suspend fun disableAllCategories() {
        categoryDao.disableAllCategories()
        // Create backup after bulk category operation
        createBackupUseCase()
    }
    
    suspend fun getEnabledCategoryCount(): Int = categoryDao.getEnabledCategoryCount()
    
    suspend fun enableAllFlashcardsInCategory(categoryId: Long) {
        flashcardDao.enableAllFlashcardsInCategory(categoryId)
        // Create backup after bulk flashcard operation
        createBackupUseCase()
    }
    
    suspend fun disableAllFlashcardsInCategory(categoryId: Long) {
        flashcardDao.disableAllFlashcardsInCategory(categoryId)
        // Create backup after bulk flashcard operation
        createBackupUseCase()
    }
    
    suspend fun getEnabledFlashcardCountByCategory(categoryId: Long): Int = 
        flashcardDao.getEnabledFlashcardCountByCategory(categoryId)
}
