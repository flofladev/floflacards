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
import com.floflacards.app.domain.usecase.ACTIVE_POOL_CAP
import com.floflacards.app.domain.usecase.MASTERY_MIN_EASINESS
import com.floflacards.app.domain.usecase.MASTERY_MIN_REVIEWS
import com.floflacards.app.domain.util.EmptyStateFlashcard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    /** Live map of categoryId -> flashcard count (categories with no cards map to 0 via the caller's default). */
    fun getFlashcardCountsPerCategory(): Flow<Map<Long, Int>> =
        flashcardDao.getFlashcardCountsPerCategory().map { counts ->
            counts.associate { it.categoryId to it.count }
        }
    
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
     * Id of the most recently shown flashcard, used to avoid showing the same
     * card twice in a row. Kept in memory only (this repository is a singleton);
     * it resets to "none" on process restart, which is fine — any card is an
     * acceptable choice after a restart.
     */
    @Volatile
    private var lastShownFlashcardId: Long = FlashcardDao.NO_EXCLUDED_CARD

    /**
     * Gets the next flashcard to show, guaranteeing a result whenever any enabled card exists
     * (otherwise the empty-state card). This implements gradual introduction: only a small
     * "active learning" set ([ACTIVE_POOL_CAP] cards) is in rotation at once, so a large deck is
     * learned a few cards at a time instead of all at once.
     *
     * Priority each draw (the previously shown card is excluded so it is never shown twice in a
     * row, unless it is the only card available):
     *  1. A card being learned that is due for review (drill the active set).
     *  2. If the active pool has room, introduce a brand-new (never-seen) card.
     *  3. A mastered card whose cooldown elapsed (maintenance review). Ranked below new
     *     introduction so a large mastered set can never starve new learning.
     *  4. The soonest-to-be-ready card; finally the just-shown card itself (single-card decks).
     *
     * Selection is stateless apart from [lastShownFlashcardId]: counts are recomputed from the DB
     * every draw, so additions/edits/disables/deletes are reflected immediately.
     */
    suspend fun getNextAvailableFlashcard(): FlashcardEntity {
        val now = System.currentTimeMillis()
        val exclude = lastShownFlashcardId

        // 1. A card currently being learned that is due for review.
        var selected = flashcardDao.getNextDueLearningCard(
            now, MASTERY_MIN_EASINESS, MASTERY_MIN_REVIEWS, exclude
        )

        // 2. Room in the active learning pool: introduce a brand-new card.
        if (selected == null &&
            flashcardDao.countActiveLearningCards(MASTERY_MIN_EASINESS, MASTERY_MIN_REVIEWS) < ACTIVE_POOL_CAP
        ) {
            selected = flashcardDao.getNextNewCard(exclude)
        }

        // 3. A mastered card due for a maintenance review.
        if (selected == null) {
            selected = flashcardDao.getNextDueMasteredCard(
                now, MASTERY_MIN_EASINESS, MASTERY_MIN_REVIEWS, exclude
            )
        }

        // 4. Nothing is due: the soonest-to-be-ready card other than the one just shown,
        //    preferring already-seen cards so a full active pool isn't bypassed.
        if (selected == null) {
            selected = flashcardDao.getFallbackCard(exclude)
        }

        // 5. The just-shown card is the only enabled card: show it rather than nothing.
        if (selected == null) {
            selected = flashcardDao.getFallbackCard(FlashcardDao.NO_EXCLUDED_CARD)
        }

        return if (selected != null) {
            lastShownFlashcardId = selected.id
            selected
        } else {
            // No enabled cards at all — show the empty state.
            EmptyStateFlashcard.create()
        }
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
