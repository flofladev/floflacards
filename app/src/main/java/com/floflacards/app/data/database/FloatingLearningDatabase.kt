package com.floflacards.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity

@Database(
    entities = [CategoryEntity::class, FlashcardEntity::class],
    version = 7,
    exportSchema = false
)
abstract class FloatingLearningDatabase : RoomDatabase() {
    
    abstract fun categoryDao(): CategoryDao
    abstract fun flashcardDao(): FlashcardDao
    
    companion object {
        const val DATABASE_NAME = "floating_learning_database"
        
        // Migration to add cooldownUntil field
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flashcards ADD COLUMN cooldownUntil INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration to add consecutiveCorrectCount field (now removed in v4)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flashcards ADD COLUMN consecutiveCorrectCount INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration to SM-2 algorithm: remove old fields, add SM-2 fields
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add SM-2 fields
                database.execSQL("ALTER TABLE flashcards ADD COLUMN easinessFactor REAL NOT NULL DEFAULT 2.5")
                database.execSQL("ALTER TABLE flashcards ADD COLUMN reviewCount INTEGER NOT NULL DEFAULT 0")
                
                // Remove old algorithm fields by creating new table and copying data
                database.execSQL("""
                    CREATE TABLE flashcards_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        question TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL,
                        incorrectCount INTEGER NOT NULL,
                        easinessFactor REAL NOT NULL,
                        reviewCount INTEGER NOT NULL,
                        lastReviewedAt INTEGER NOT NULL,
                        cooldownUntil INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                
                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO flashcards_new (
                        id, categoryId, question, answer, isEnabled, correctCount, incorrectCount,
                        easinessFactor, reviewCount, lastReviewedAt, cooldownUntil, createdAt, updatedAt
                    )
                    SELECT 
                        id, categoryId, question, answer, isEnabled, correctCount, incorrectCount,
                        2.5, 0, lastReviewedAt, cooldownUntil, createdAt, updatedAt
                    FROM flashcards
                """)
                
                // Drop old table and rename new table
                database.execSQL("DROP TABLE flashcards")
                database.execSQL("ALTER TABLE flashcards_new RENAME TO flashcards")
                
                // Recreate index
                database.execSQL("CREATE INDEX index_flashcards_categoryId ON flashcards(categoryId)")
            }
        }
        
        // Migration to add hardCount field for tracking "Hard" button presses
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flashcards ADD COLUMN hardCount INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration to add image path fields for flashcard images
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flashcards ADD COLUMN questionImagePath TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE flashcards ADD COLUMN answerImagePath TEXT DEFAULT NULL")
            }
        }
        
        // Migration to clean up schema and remove old fields
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with correct schema
                database.execSQL("""
                    CREATE TABLE flashcards_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        question TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL,
                        incorrectCount INTEGER NOT NULL,
                        hardCount INTEGER NOT NULL,
                        easinessFactor REAL NOT NULL,
                        reviewCount INTEGER NOT NULL,
                        lastReviewedAt INTEGER NOT NULL,
                        cooldownUntil INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                
                // Copy data from old table to new table, handling missing columns
                database.execSQL("""
                    INSERT INTO flashcards_new (
                        id, categoryId, question, answer, isEnabled, correctCount, incorrectCount,
                        hardCount, easinessFactor, reviewCount, lastReviewedAt, cooldownUntil, createdAt, updatedAt
                    )
                    SELECT 
                        id, categoryId, question, answer, isEnabled, correctCount, incorrectCount,
                        COALESCE(hardCount, 0), easinessFactor, reviewCount, lastReviewedAt, cooldownUntil, createdAt, updatedAt
                    FROM flashcards
                """)
                
                // Drop old table and rename new table
                database.execSQL("DROP TABLE flashcards")
                database.execSQL("ALTER TABLE flashcards_new RENAME TO flashcards")
                
                // Recreate index
                database.execSQL("CREATE INDEX index_flashcards_categoryId ON flashcards(categoryId)")
            }
        }
    }
}
