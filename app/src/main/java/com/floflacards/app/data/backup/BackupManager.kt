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

package com.floflacards.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import java.io.File
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.database.FloatingLearningDatabase
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.data.source.StreakPreferences
import com.floflacards.app.domain.model.StreakData
import com.floflacards.app.util.ImageUtils
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
    private val database: FloatingLearningDatabase,
    private val flashcardDao: FlashcardDao,
    private val categoryDao: CategoryDao,
    private val streakPreferences: StreakPreferences,
    private val backupPreferences: BackupPreferences
) {
    companion object {
        private const val BACKUP_FILENAME = "backup.json"
        // Temp file used for atomic writes: full content is written here first,
        // then renamed over backup.json so sync tools never observe a partial file.
        private const val BACKUP_TMP_FILENAME = "backup.json.tmp"
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
     * Writes the backup JSON atomically into [treeDocument].
     *
     * Strategy: write the complete payload to a temp file, delete the previous
     * backup.json, then rename the temp file into place. On file-based SAF
     * providers (the kind used by local Syncthing/Nextcloud folders) the rename
     * is atomic, so a syncing client never observes a partially-written
     * backup.json. If the provider doesn't support rename, falls back to writing
     * a fresh backup.json and removing the temp file.
     *
     * @return the final document URI string, or null on failure.
     */
    private fun writeBackupAtomically(treeDocument: DocumentFile, jsonString: String): String? {
        // Remove any stale temp file so createFile doesn't produce "backup.json.tmp (1)".
        treeDocument.findFile(BACKUP_TMP_FILENAME)?.delete()

        val tmpDoc = treeDocument.createFile("application/json", BACKUP_TMP_FILENAME) ?: return null

        val wrote = try {
            context.contentResolver.openOutputStream(tmpDoc.uri, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(jsonString)
            } != null
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Failed writing temp backup", e)
            false
        }
        if (!wrote) {
            tmpDoc.delete()
            return null
        }

        // The complete temp file now exists on disk. Remove the old backup and
        // promote the temp file to the final name.
        treeDocument.findFile(BACKUP_FILENAME)?.delete()

        val renamed = try {
            tmpDoc.renameTo(BACKUP_FILENAME)
        } catch (e: Exception) {
            false
        }
        if (renamed) {
            return treeDocument.findFile(BACKUP_FILENAME)?.uri?.toString() ?: tmpDoc.uri.toString()
        }

        // Fallback: provider doesn't support rename — write a fresh final file.
        val finalDoc = treeDocument.createFile("application/json", BACKUP_FILENAME) ?: return null
        val fallbackWrote = try {
            context.contentResolver.openOutputStream(finalDoc.uri, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(jsonString)
            } != null
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Failed writing final backup (fallback)", e)
            false
        }
        tmpDoc.delete()
        return if (fallbackWrote) finalDoc.uri.toString() else null
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
    private fun copyImageToBackup(
        imagePath: String?,
        imagesFolder: DocumentFile,
        existingNames: MutableSet<String>
    ): String? {
        if (imagePath == null) return null

        val sourceFile = File(imagePath)
        if (!sourceFile.exists()) return null

        try {
            // Content-addressed name: identical image bytes always map to the same file.
            val hash = ImageUtils.sha256(sourceFile) ?: return null
            val filename = "$hash.jpg"
            val relativePath = "$IMAGES_FOLDER/$filename"

            // Already present with this exact content → reuse it, write nothing. This is what
            // keeps re-backups from rewriting unchanged images (no sync churn / notifications)
            // and what prevents duplicate copies accumulating across restores.
            if (filename in existingNames) return relativePath

            val backupFile = imagesFolder.createFile("image/jpeg", filename) ?: return null
            context.contentResolver.openOutputStream(backupFile.uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            existingNames.add(filename)
            return relativePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Deletes any file in the backup images folder that no current card references.
     * Removes images from deleted cards and legacy duplicate copies from older versions.
     * Only ever deletes, so an unchanged card set leaves the folder byte-identical.
     * Must run AFTER backup.json is written, so the manifest never points at a pruned file.
     */
    private fun pruneBackupImages(imagesFolder: DocumentFile, referencedNames: Set<String>) {
        try {
            for (file in imagesFolder.listFiles()) {
                val name = file.name ?: continue
                if (name == ".nomedia" || !file.isFile) continue // never touch the gallery guard
                if (name !in referencedNames) file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Failed pruning backup images", e)
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

            // Resolve the images folder once and snapshot its existing filenames, so each card's
            // image check is an in-memory lookup instead of a per-call SAF directory scan
            // (O(n) instead of O(n²) — noticeably faster on slow/older storage).
            val imagesFolder = treeDocument.findFile(IMAGES_FOLDER)
                ?: treeDocument.createDirectory(IMAGES_FOLDER)
            val existingImageNames: MutableSet<String> =
                (imagesFolder?.listFiles() ?: emptyArray()).mapNotNull { it.name }.toMutableSet()
            imagesFolder?.let { ensureNoMediaInImagesFolder(it) }

            val flashcardBackups = flashcards.map { flashcard ->
                // Copy images to backup folder and get relative paths (content-addressed).
                val questionImageBackupPath = imagesFolder?.let {
                    copyImageToBackup(flashcard.questionImagePath, it, existingImageNames)
                }
                val answerImageBackupPath = imagesFolder?.let {
                    copyImageToBackup(flashcard.answerImagePath, it, existingImageNames)
                }
                
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

            // Write atomically: full content to a temp file, then rename over
            // backup.json. This guarantees a syncing client (Syncthing/Nextcloud)
            // never picks up a half-written backup.json.
            val jsonString = json.encodeToString(backupData)
            val finalUri = writeBackupAtomically(treeDocument, jsonString)
                ?: return@withContext Result.failure(IllegalStateException("Cannot write backup document"))

            // Prune only after the manifest is safely written, so backup.json can never
            // reference an image we just deleted. Removes orphaned/duplicate image files.
            imagesFolder?.let {
                val referencedNames = flashcardBackups
                    .flatMap { card -> listOf(card.questionImagePath, card.answerImagePath) }
                    .filterNotNull()
                    .map { path -> path.substringAfterLast('/') }
                    .toSet()
                pruneBackupImages(it, referencedNames)
            }

            Result.success(finalUri)
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

            // REPLACE MODE wrapped in a single transaction: either the whole
            // restore succeeds or the database is left untouched. This fixes the
            // prior data-loss bug where a crash after clearing — but before
            // re-inserting — would wipe all of the user's cards.
            val (categoriesRestored, flashcardsRestored) = database.withTransaction {
                // Clear flashcards first (foreign key constraints), then categories.
                flashcardDao.deleteAllFlashcards()
                categoryDao.deleteAllCategories()

                var categoriesCount = 0
                var flashcardsCount = 0

                // Restore all categories from backup
                for (categoryBackup in backupData.categories) {
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = categoryBackup.name,
                            isEnabled = categoryBackup.isEnabled,
                            createdAt = categoryBackup.createdAt,
                            updatedAt = categoryBackup.updatedAt
                        )
                    )
                    categoriesCount++
                }

                // Map restored category names to their new IDs for flashcard linking.
                val categoryNameToIdMap = categoryDao.getAllCategoriesForBackup()
                    .associate { it.name to it.id }

                // Restore all flashcards from backup
                for (flashcardBackup in backupData.flashcards) {
                    // Find category by name (more reliable than UUID for restore)
                    val categoryName = backupData.categories.find {
                        it.uuid == flashcardBackup.categoryUuid
                    }?.name
                    val categoryId = categoryName?.let { categoryNameToIdMap[it] }

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
                            flashcardBackup.questionImagePath, treeDocument, insertedId, isQuestion = true
                        )
                        val restoredAnswerPath = restoreImageFromBackup(
                            flashcardBackup.answerImagePath, treeDocument, insertedId, isQuestion = false
                        )

                        // Update flashcard with restored image paths if any were restored
                        if (restoredQuestionPath != null || restoredAnswerPath != null) {
                            flashcardDao.updateFlashcard(
                                flashcardEntity.copy(
                                    id = insertedId,
                                    questionImagePath = restoredQuestionPath,
                                    answerImagePath = restoredAnswerPath
                                )
                            )
                        }
                        flashcardsCount++
                    }
                }

                Pair(categoriesCount, flashcardsCount)
            }

            // Restore streak data after the DB transaction commits. Done outside
            // the transaction because streak lives in SharedPreferences, which
            // Room can't roll back — we only touch it once the data is safely in.
            backupData.streakData?.let { streakBackup ->
                val backupStreakData = StreakData(
                    currentStreak = streakBackup.currentStreak,
                    highestStreak = streakBackup.highestStreak,
                    lastActivityTimestamp = streakBackup.lastActivityTimestamp
                )

                // Apply gap detection: if the streak is stale, preserve highest but reset current.
                val validCurrentStreak = backupStreakData.getCurrentValidStreak(System.currentTimeMillis())
                val finalStreakData = if (validCurrentStreak == 0 && streakBackup.currentStreak > 0) {
                    StreakData(
                        currentStreak = 0,
                        highestStreak = streakBackup.highestStreak,
                        lastActivityTimestamp = streakBackup.lastActivityTimestamp
                    )
                } else {
                    backupStreakData
                }
                streakPreferences.saveStreakData(finalStreakData)
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
