package com.floflacards.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.data.source.StreakPreferences
import com.floflacards.app.domain.model.StreakData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SAF-based backup manager following SOLID principles.
 * Single responsibility: Handle backup creation, restoration, and file operations using SAF.
 */
@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val flashcardDao: FlashcardDao,
    private val categoryDao: CategoryDao,
    private val streakPreferences: StreakPreferences,
    private val backupPreferences: BackupPreferences
) {
    companion object {
        private const val BACKUP_FILENAME = "backup.json"
        private const val IMAGES_FOLDER = "images"
    }
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Gets the backup document using SAF.
     * Returns null if SAF tree URI is not configured.
     */
    private fun getBackupDocument(): DocumentFile? {
        val treeUriString = backupPreferences.getSafTreeUri() ?: return null
        val treeUri = Uri.parse(treeUriString)
        val treeDocument = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        
        // Find existing backup file - don't create it here to distinguish between exists/not exists
        return treeDocument.findFile(BACKUP_FILENAME)
    }

    /**
     * Gets or creates the backup document using SAF.
     */
    private fun getOrCreateBackupDocument(): DocumentFile? {
        val treeUriString = backupPreferences.getSafTreeUri() ?: return null
        val treeUri = Uri.parse(treeUriString)
        val treeDocument = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        
        // Find existing backup file or create new one
        return treeDocument.findFile(BACKUP_FILENAME) 
            ?: treeDocument.createFile("application/json", BACKUP_FILENAME)
    }

    /**
     * Checks if backup file exists.
     */
    suspend fun hasExistingBackup(): Boolean = withContext(Dispatchers.IO) {
        val backupDocument = getBackupDocument()
        backupDocument?.exists() == true
    }

    /**
     * Gets backup file information for UI display.
     */
    suspend fun getBackupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        val backupDocument = getBackupDocument()
        
        if (backupDocument == null || !backupDocument.exists()) {
            return@withContext BackupInfo(
                exists = false,
                filePath = "No backup folder selected"
            )
        }

        try {
            val inputStream = context.contentResolver.openInputStream(backupDocument.uri)
            val backupContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            val backupData = json.decodeFromString<BackupData>(backupContent)
            
            BackupInfo(
                exists = true,
                filePath = backupDocument.uri.toString(),
                createdAt = backupData.createdAt,
                updatedAt = backupData.updatedAt,
                categoryCount = backupData.categories.size,
                flashcardCount = backupData.flashcards.size,
                totalReviews = backupData.metadata.totalReviews,
                fileSize = backupDocument.length()
            )
        } catch (e: Exception) {
            BackupInfo(
                exists = true,
                filePath = backupDocument.uri.toString(),
                fileSize = backupDocument.length()
            )
        }
    }

    /**
     * Ensure .nomedia file exists in images folder to prevent gallery indexing.
     */
    private fun ensureNoMediaInImagesFolder(imagesFolder: DocumentFile) {
        try {
            val noMediaFile = imagesFolder.findFile(".nomedia")
            if (noMediaFile == null) {
                // Create .nomedia file
                val created = imagesFolder.createFile("application/octet-stream", ".nomedia")
                if (created != null) {
                    android.util.Log.d("BackupManager", ".nomedia file created in backup images folder")
                } else {
                    android.util.Log.e("BackupManager", "Failed to create .nomedia file in backup folder")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Error creating .nomedia in backup folder", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Copies an image file to backup images folder.
     * Returns the relative path (e.g., "images/question_123.jpg") or null if copy fails.
     */
    private fun copyImageToBackup(imagePath: String?, treeDocument: DocumentFile): String? {
        if (imagePath == null) return null
        
        val sourceFile = File(imagePath)
        if (!sourceFile.exists()) return null
        
        try {
            // Get or create images subfolder
            val imagesFolder = treeDocument.findFile(IMAGES_FOLDER) 
                ?: treeDocument.createDirectory(IMAGES_FOLDER) ?: return null
            
            // Ensure .nomedia file exists to prevent gallery indexing
            ensureNoMediaInImagesFolder(imagesFolder)
            
            // Generate filename from source
            val filename = sourceFile.name
            
            // Delete existing file if it exists (overwrite)
            imagesFolder.findFile(filename)?.delete()
            
            // Create new file in backup images folder
            val backupFile = imagesFolder.createFile("image/jpeg", filename) ?: return null
            
            // Copy file content
            context.contentResolver.openOutputStream(backupFile.uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            
            // Return relative path for JSON
            return "$IMAGES_FOLDER/$filename"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Restores an image from backup folder to internal storage.
     * Returns the new internal storage path or null if restore fails.
     */
    private fun restoreImageFromBackup(relativePath: String?, treeDocument: DocumentFile, flashcardId: Long, isQuestion: Boolean): String? {
        if (relativePath == null) return null
        
        try {
            // Find the image in backup folder
            val pathParts = relativePath.split("/")
            if (pathParts.size != 2 || pathParts[0] != IMAGES_FOLDER) return null
            
            val filename = pathParts[1]
            val imagesFolder = treeDocument.findFile(IMAGES_FOLDER) ?: return null
            
            // Ensure .nomedia exists in backup images folder
            ensureNoMediaInImagesFolder(imagesFolder)
            
            val backupFile = imagesFolder.findFile(filename) ?: return null
            
            // Create new filename for internal storage
            val prefix = if (isQuestion) "question" else "answer"
            val timestamp = System.currentTimeMillis()
            val newFilename = "${prefix}_${flashcardId}_${timestamp}.jpg"
            
            // Create internal storage directory
            val internalDir = File(context.filesDir, "flashcard_images")
            internalDir.mkdirs()
            
            val destFile = File(internalDir, newFilename)
            
            // Copy file from backup to internal storage
            context.contentResolver.openInputStream(backupFile.uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            return destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Creates or updates backup file using SAF.
     */
    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if SAF folder is configured
            if (!backupPreferences.hasSafFolderConfigured()) {
                return@withContext Result.failure(
                    IllegalStateException("Backup folder not selected")
                )
            }

            // Get tree document for image copying
            val treeUriString = backupPreferences.getSafTreeUri()!!
            val treeUri = Uri.parse(treeUriString)
            val treeDocument = DocumentFile.fromTreeUri(context, treeUri)!!
            
            // Get all data from database
            val categories = categoryDao.getAllCategoriesForBackup()
            val flashcards = flashcardDao.getAllFlashcardsForStatistics()

            // Create UUID mapping for categories
            val categoryUuidMap = categories.associate { it.id to UUID.randomUUID().toString() }

            // Convert to backup format
            val categoryBackups = categories.map { category ->
                CategoryBackup(
                    id = category.id,
                    uuid = categoryUuidMap[category.id] ?: UUID.randomUUID().toString(),
                    name = category.name,
                    isEnabled = category.isEnabled,
                    createdAt = category.createdAt,
                    updatedAt = category.updatedAt
                )
            }

            val flashcardBackups = flashcards.map { flashcard ->
                // Copy images to backup folder and get relative paths
                val questionImageBackupPath = copyImageToBackup(flashcard.questionImagePath, treeDocument)
                val answerImageBackupPath = copyImageToBackup(flashcard.answerImagePath, treeDocument)
                
                FlashcardBackup(
                    id = flashcard.id,
                    uuid = UUID.randomUUID().toString(),
                    categoryId = flashcard.categoryId,
                    categoryUuid = categoryUuidMap[flashcard.categoryId] ?: "",
                    question = flashcard.question,
                    answer = flashcard.answer,
                    questionImagePath = questionImageBackupPath,  // Relative path in backup folder
                    answerImagePath = answerImageBackupPath,       // Relative path in backup folder
                    isEnabled = flashcard.isEnabled,
                    correctCount = flashcard.correctCount,
                    incorrectCount = flashcard.incorrectCount,
                    hardCount = flashcard.hardCount,
                    easinessFactor = flashcard.easinessFactor,
                    reviewCount = flashcard.reviewCount,
                    lastReviewedAt = flashcard.lastReviewedAt,
                    cooldownUntil = flashcard.cooldownUntil,
                    createdAt = flashcard.createdAt,
                    updatedAt = flashcard.updatedAt
                )
            }

            // Calculate total reviews
            val totalReviews = flashcards.sumOf { it.correctCount + it.incorrectCount }

            // Get current streak data
            val currentStreakData = streakPreferences.getStreakData()
            val streakBackup = StreakBackup(
                currentStreak = currentStreakData.currentStreak,
                highestStreak = currentStreakData.highestStreak,
                lastActivityTimestamp = currentStreakData.lastActivityTimestamp
            )

            // Create backup data
            val backupData = BackupData(
                categories = categoryBackups,
                flashcards = flashcardBackups,
                streakData = streakBackup,
                metadata = BackupMetadata(
                    totalCategories = categories.size,
                    totalFlashcards = flashcards.size,
                    totalReviews = totalReviews
                )
            )

            // Write to SAF document
            val backupDocument = getOrCreateBackupDocument() 
                ?: return@withContext Result.failure(IllegalStateException("Cannot create backup document"))
                
            val jsonString = json.encodeToString(backupData)
            val outputStream = context.contentResolver.openOutputStream(backupDocument.uri, "wt")
            outputStream?.bufferedWriter()?.use { writer ->
                writer.write(jsonString)
            }

            Result.success(backupDocument.uri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores data from backup file.
     */
    suspend fun restoreBackup(): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val backupDocument = getBackupDocument()
            if (backupDocument == null || !backupDocument.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Backup file not found")
                )
            }

            // Get tree document for image restoration
            val treeUriString = backupPreferences.getSafTreeUri()!!
            val treeUri = Uri.parse(treeUriString)
            val treeDocument = DocumentFile.fromTreeUri(context, treeUri)!!
            
            val inputStream = context.contentResolver.openInputStream(backupDocument.uri)
            val backupContent = inputStream?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(IllegalStateException("Cannot read backup file"))
            val backupData = json.decodeFromString<BackupData>(backupContent)

            // Debug: Log backup data info
            println("DEBUG: Backup contains ${backupData.categories.size} categories and ${backupData.flashcards.size} flashcards")
        
            // REPLACE MODE: Clear all existing data and restore everything from backup
            println("DEBUG: Using REPLACE mode - clearing all existing data")
        
            // Clear all flashcards first (due to foreign key constraints)
            flashcardDao.deleteAllFlashcards()
            println("DEBUG: Cleared all existing flashcards")
        
            // Clear all categories
            categoryDao.deleteAllCategories()
            println("DEBUG: Cleared all existing categories")
        
            var categoriesRestored = 0
            var flashcardsRestored = 0

            // Restore all categories from backup
            println("DEBUG: Restoring ${backupData.categories.size} categories")
            for (categoryBackup in backupData.categories) {
                println("DEBUG: Restoring category '${categoryBackup.name}'")
                val categoryEntity = CategoryEntity(
                    name = categoryBackup.name,
                    isEnabled = categoryBackup.isEnabled,
                    createdAt = categoryBackup.createdAt,
                    updatedAt = categoryBackup.updatedAt
                )
                categoryDao.insertCategory(categoryEntity)
                categoriesRestored++
                println("DEBUG: Category '${categoryBackup.name}' restored successfully")
            }

            // Get all restored categories for flashcard mapping
            val allCategories = categoryDao.getAllCategoriesForBackup()
            val categoryNameToIdMap = allCategories.associate { it.name to it.id }
            println("DEBUG: Category mapping created: $categoryNameToIdMap")

            // Restore all flashcards from backup
            println("DEBUG: Restoring ${backupData.flashcards.size} flashcards")
            for (flashcardBackup in backupData.flashcards) {
                println("DEBUG: Restoring flashcard '${flashcardBackup.question}'")
            
                // Find category by name (more reliable than UUID for restore)
                val categoryName = backupData.categories.find { 
                    it.uuid == flashcardBackup.categoryUuid 
                }?.name
            
                val categoryId = categoryName?.let { categoryNameToIdMap[it] }
                println("DEBUG: Mapped flashcard to category ID: $categoryId")
            
                if (categoryId != null) {
                    // Insert flashcard first to get the ID
                    val flashcardEntity = FlashcardEntity(
                        categoryId = categoryId,
                        question = flashcardBackup.question,
                        answer = flashcardBackup.answer,
                        questionImagePath = null,  // Will be updated after image restore
                        answerImagePath = null,     // Will be updated after image restore
                        isEnabled = flashcardBackup.isEnabled,
                        correctCount = flashcardBackup.correctCount,
                        incorrectCount = flashcardBackup.incorrectCount,
                        hardCount = flashcardBackup.hardCount,
                        easinessFactor = flashcardBackup.easinessFactor,
                        reviewCount = flashcardBackup.reviewCount,
                        lastReviewedAt = flashcardBackup.lastReviewedAt,
                        cooldownUntil = flashcardBackup.cooldownUntil,
                        createdAt = flashcardBackup.createdAt,
                        updatedAt = flashcardBackup.updatedAt
                    )
                    val insertedId = flashcardDao.insertFlashcard(flashcardEntity)
                    
                    // Restore images from backup folder to internal storage
                    val restoredQuestionPath = restoreImageFromBackup(
                        flashcardBackup.questionImagePath, 
                        treeDocument, 
                        insertedId, 
                        isQuestion = true
                    )
                    val restoredAnswerPath = restoreImageFromBackup(
                        flashcardBackup.answerImagePath, 
                        treeDocument, 
                        insertedId, 
                        isQuestion = false
                    )
                    
                    // Update flashcard with restored image paths if any were restored
                    if (restoredQuestionPath != null || restoredAnswerPath != null) {
                        val updatedEntity = flashcardEntity.copy(
                            id = insertedId,
                            questionImagePath = restoredQuestionPath,
                            answerImagePath = restoredAnswerPath
                        )
                        flashcardDao.updateFlashcard(updatedEntity)
                    }
                    
                    flashcardsRestored++
                    println("DEBUG: Flashcard '${flashcardBackup.question}' restored successfully")
                } else {
                    println("DEBUG: ERROR - Could not find category ID for flashcard '${flashcardBackup.question}'")
                }
            }

            // Restore streak data with gap detection logic
            backupData.streakData?.let { streakBackup ->
                println("DEBUG: Restoring streak data - Current: ${streakBackup.currentStreak}, Highest: ${streakBackup.highestStreak}")
                
                // Create StreakData from backup
                val backupStreakData = StreakData(
                    currentStreak = streakBackup.currentStreak,
                    highestStreak = streakBackup.highestStreak,
                    lastActivityTimestamp = streakBackup.lastActivityTimestamp
                )
                
                // Apply gap detection - use existing logic in StreakData.getCurrentValidStreak()
                val currentTime = System.currentTimeMillis()
                val validCurrentStreak = backupStreakData.getCurrentValidStreak(currentTime)
                
                // If there's a gap (validCurrentStreak = 0), preserve highest but reset current
                val finalStreakData = if (validCurrentStreak == 0 && streakBackup.currentStreak > 0) {
                    println("DEBUG: Gap detected - resetting current streak but preserving highest")
                    StreakData(
                        currentStreak = 0,
                        highestStreak = streakBackup.highestStreak,
                        lastActivityTimestamp = streakBackup.lastActivityTimestamp
                    )
                } else {
                    println("DEBUG: No gap detected - restoring full streak data")
                    backupStreakData
                }
                
                // Save the processed streak data
                streakPreferences.saveStreakData(finalStreakData)
                println("DEBUG: Streak data restored successfully")
            } ?: run {
                println("DEBUG: No streak data found in backup (backward compatibility)")
            }

            Result.success(
                RestoreResult(
                    success = true,
                    categoriesRestored = categoriesRestored,
                    flashcardsRestored = flashcardsRestored
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes the backup file.
     */
    suspend fun deleteBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val backupDocument = getBackupDocument()
            val deleted = if (backupDocument?.exists() == true) {
                backupDocument.delete()
            } else {
                true // Already doesn't exist
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
