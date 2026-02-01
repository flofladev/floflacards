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

package com.floflacards.app.data.dao

import androidx.room.*
import com.floflacards.app.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE isEnabled = 1 ORDER BY createdAt ASC")
    fun getEnabledCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?
    
    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
    
    // Backup-specific methods
    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
    suspend fun getAllCategoriesForBackup(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
    
    // Bulk operations for select/deselect all functionality
    @Query("UPDATE categories SET isEnabled = 1, updatedAt = :timestamp")
    suspend fun enableAllCategories(timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE categories SET isEnabled = 0, updatedAt = :timestamp")
    suspend fun disableAllCategories(timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM categories WHERE isEnabled = 1")
    suspend fun getEnabledCategoryCount(): Int
}
