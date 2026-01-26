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
