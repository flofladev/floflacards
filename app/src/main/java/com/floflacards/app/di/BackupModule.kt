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
