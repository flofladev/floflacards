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
import com.floflacards.app.data.source.BackupPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val flashcardDao: FlashcardDao,
    private val backupPreferences: BackupPreferences
) {
    /**
     * Marks the dataset as changed since the last backup. This is a cheap
     * SharedPreferences increment (no file I/O) — the actual backup is performed
     * later by [com.floflacards.app.data.backup.BackupWorker] on a schedule and
     * when the app goes to the background. Backups are NOT written synchronously
     * on every change: SQLite already makes each write durable, and writing the
     * full backup file on every edit caused excessive sync traffic and corruption
     * risk for users syncing the backup folder (Syncthing/Nextcloud).
     */
    private fun markDataChanged() {
        backupPreferences.incrementDataVersion()
    }
    
    // Category operations
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    
    fun getEnabledCategories(): Flow<List<CategoryEntity>> = categoryDao.getEnabledCategories()
    
    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)
    
    suspend fun insertCategory(category: CategoryEntity): Long {
        val result = categoryDao.insertCategory(category)
        // Mark data changed (backup deferred to worker) after category creation
        markDataChanged()
        return result
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
        // Mark data changed (backup deferred to worker) after category update
        markDataChanged()
    }
    
    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
        // Mark data changed (backup deferred to worker) after category deletion
        markDataChanged()
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
        // Mark data changed (backup deferred to worker) after flashcard creation
        markDataChanged()
        return result
    }
    
    suspend fun updateFlashcard(flashcard: FlashcardEntity) {
        flashcardDao.updateFlashcard(flashcard)
        // Mark data changed (backup deferred to worker) after flashcard update
        markDataChanged()
    }
    
    suspend fun deleteFlashcard(flashcard: FlashcardEntity) {
        flashcardDao.deleteFlashcard(flashcard)
        // Mark data changed (backup deferred to worker) after flashcard deletion
        markDataChanged()
    }
    
    // Statistics reset operations
    suspend fun resetFlashcardStatistics(flashcardId: Long) {
        flashcardDao.resetFlashcardStatistics(flashcardId)
        // Mark data changed (backup deferred to worker) after statistics reset
        markDataChanged()
    }
    
    suspend fun resetCategoryStatistics(categoryId: Long) {
        flashcardDao.resetCategoryStatistics(categoryId)
        // Mark data changed (backup deferred to worker) after statistics reset
        markDataChanged()
    }
    
    suspend fun resetAllStatistics() {
        flashcardDao.resetAllStatistics()
        // Mark data changed (backup deferred to worker) after statistics reset
        markDataChanged()
    }
    
    // Bulk operations for select/deselect all functionality
    suspend fun enableAllCategories() {
        categoryDao.enableAllCategories()
        // Mark data changed (backup deferred to worker) after bulk category operation
        markDataChanged()
    }
    
    suspend fun disableAllCategories() {
        categoryDao.disableAllCategories()
        // Mark data changed (backup deferred to worker) after bulk category operation
        markDataChanged()
    }
    
    suspend fun getEnabledCategoryCount(): Int = categoryDao.getEnabledCategoryCount()
    
    suspend fun enableAllFlashcardsInCategory(categoryId: Long) {
        flashcardDao.enableAllFlashcardsInCategory(categoryId)
        // Mark data changed (backup deferred to worker) after bulk flashcard operation
        markDataChanged()
    }
    
    suspend fun disableAllFlashcardsInCategory(categoryId: Long) {
        flashcardDao.disableAllFlashcardsInCategory(categoryId)
        // Mark data changed (backup deferred to worker) after bulk flashcard operation
        markDataChanged()
    }
    
    suspend fun getEnabledFlashcardCountByCategory(categoryId: Long): Int = 
        flashcardDao.getEnabledFlashcardCountByCategory(categoryId)
}
