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
import android.content.SharedPreferences
import androidx.room.Room
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.database.FloatingLearningDatabase
import com.floflacards.app.util.PermissionHelper
import com.floflacards.app.data.source.ImageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FloatingLearningDatabase {
        return Room.databaseBuilder(
            context,
            FloatingLearningDatabase::class.java,
            FloatingLearningDatabase.DATABASE_NAME
        )
        .addMigrations(
            FloatingLearningDatabase.MIGRATION_1_2,
            FloatingLearningDatabase.MIGRATION_2_3,
            FloatingLearningDatabase.MIGRATION_3_4,
            FloatingLearningDatabase.MIGRATION_4_5,
            FloatingLearningDatabase.MIGRATION_5_6,
            FloatingLearningDatabase.MIGRATION_6_7
        )
        .build()
    }
    
    @Provides
    fun provideCategoryDao(database: FloatingLearningDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun provideFlashcardDao(database: FloatingLearningDatabase): FlashcardDao {
        return database.flashcardDao()
    }
    
    @Provides
    fun providePermissionHelper(@ApplicationContext context: Context): PermissionHelper {
        return PermissionHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("floating_learning_settings", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideImageManager(@ApplicationContext context: Context): ImageManager {
        return ImageManager(context)
    }
}
