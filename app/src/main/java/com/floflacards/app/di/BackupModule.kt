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

package com.floflacards.app.di

import android.content.Context
import com.floflacards.app.data.backup.BackupManager
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.data.source.StreakPreferences
import com.floflacards.app.data.repository.BackupRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for backup functionality.
 * Follows SOLID principles with proper dependency management.
 */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        flashcardDao: FlashcardDao,
        categoryDao: CategoryDao,
        streakPreferences: StreakPreferences,
        backupPreferences: BackupPreferences
    ): BackupManager {
        return BackupManager(
            context = context,
            flashcardDao = flashcardDao,
            categoryDao = categoryDao,
            streakPreferences = streakPreferences,
            backupPreferences = backupPreferences
        )
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        backupManager: BackupManager
    ): BackupRepository {
        return BackupRepository(backupManager)
    }

    @Provides
    @Singleton
    fun provideBackupPreferences(
        @ApplicationContext context: Context
    ): BackupPreferences {
        return BackupPreferences(context)
    }
}
