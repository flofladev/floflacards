package com.floflacards.app.data.source

import android.content.Context
import android.net.Uri
import com.floflacards.app.util.ImageUtils
import java.io.File

/**
 * Manager for flashcard image file operations.
 * Handles saving, deleting, and copying images to/from internal storage.
 * 
 * Storage structure: /data/data/com.floflacards.app/files/flashcard_images/
 */
class ImageManager(private val context: Context) {
    
    companion object {
        private const val IMAGE_DIRECTORY = "flashcard_images"
        private const val QUESTION_PREFIX = "question"
        private const val ANSWER_PREFIX = "answer"
    }
    
    init {
        // Ensure directory and .nomedia file exist on initialization
        ensureImageDirectory()
    }
    
    /**
     * Ensure the image directory exists and contains .nomedia file.
     * This prevents images from appearing in the device gallery.
     */
    private fun ensureImageDirectory(): File {
        val imageDir = File(context.filesDir, IMAGE_DIRECTORY)
        if (!imageDir.exists()) {
            imageDir.mkdirs()
            android.util.Log.d("ImageManager", "Created image directory: ${imageDir.absolutePath}")
        }
        
        // Create .nomedia file to hide from gallery
        val noMediaFile = File(imageDir, ".nomedia")
        if (!noMediaFile.exists()) {
            try {
                val created = noMediaFile.createNewFile()
                android.util.Log.d("ImageManager", ".nomedia file created: $created at ${noMediaFile.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("ImageManager", "Failed to create .nomedia file", e)
                e.printStackTrace()
            }
        } else {
            android.util.Log.d("ImageManager", ".nomedia file already exists at ${noMediaFile.absolutePath}")
        }
        
        return imageDir
    }
    
    /**
     * Save an image from URI to internal storage.
     * 
     * @param uri Source image URI from image picker
     * @param flashcardId ID of the flashcard
     * @param isQuestion True for question image, false for answer image
     * @return Absolute path to saved image, or null if failed
     */
    suspend fun saveImage(uri: Uri, flashcardId: Long, isQuestion: Boolean): String? {
        return try {
            // Compress the image
            val bitmap = ImageUtils.compressImage(context, uri) ?: return null
            
            // Generate filename
            val prefix = if (isQuestion) QUESTION_PREFIX else ANSWER_PREFIX
            val timestamp = System.currentTimeMillis()
            val filename = "${prefix}_${flashcardId}_${timestamp}.jpg"
            
            // Create file in internal storage with .nomedia protection
            val imageDir = ensureImageDirectory()
            val imageFile = File(imageDir, filename)
            
            // Save bitmap to file
            val success = ImageUtils.saveBitmapToFile(bitmap, imageFile)
            
            if (success) {
                android.util.Log.d("ImageManager", "Image saved to: ${imageFile.absolutePath}")
                imageFile.absolutePath
            } else {
                android.util.Log.e("ImageManager", "Failed to save image")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete an image file from internal storage.
     * 
     * @param imagePath Absolute path to the image file
     * @return True if deleted successfully, false otherwise
     */
    fun deleteImage(imagePath: String?): Boolean {
        if (imagePath.isNullOrBlank()) return false
        
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Delete all images associated with a flashcard.
     * 
     * @param questionImagePath Question image path (nullable)
     * @param answerImagePath Answer image path (nullable)
     */
    fun deleteFlashcardImages(questionImagePath: String?, answerImagePath: String?) {
        deleteImage(questionImagePath)
        deleteImage(answerImagePath)
    }
    
    /**
     * Copy an image file (used during backup/restore operations).
     * 
     * @param sourcePath Source image path
     * @param flashcardId New flashcard ID
     * @param isQuestion True for question image, false for answer image
     * @return New image path or null if failed
     */
    fun copyImage(sourcePath: String, flashcardId: Long, isQuestion: Boolean): String? {
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null
            
            // Generate new filename
            val prefix = if (isQuestion) QUESTION_PREFIX else ANSWER_PREFIX
            val timestamp = System.currentTimeMillis()
            val filename = "${prefix}_${flashcardId}_${timestamp}.jpg"
            
            // Create destination file with .nomedia protection
            val imageDir = ensureImageDirectory()
            val destFile = File(imageDir, filename)
            
            // Copy file
            sourceFile.copyTo(destFile, overwrite = true)
            
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if an image file exists.
     * 
     * @param imagePath Image path to check
     * @return True if file exists, false otherwise
     */
    fun imageExists(imagePath: String?): Boolean {
        if (imagePath.isNullOrBlank()) return false
        return File(imagePath).exists()
    }
    
    /**
     * Get the size of an image file in bytes.
     * 
     * @param imagePath Image path
     * @return File size in bytes, or 0 if file doesn't exist
     */
    fun getImageSize(imagePath: String?): Long {
        if (imagePath.isNullOrBlank()) return 0
        val file = File(imagePath)
        return if (file.exists()) file.length() else 0
    }
    
    /**
     * Delete all orphaned image files (images not referenced by any flashcard).
     * This is a maintenance operation that should be called periodically.
     * Note: Implementation would require database access, so this is a placeholder.
     * 
     * @return Number of files deleted
     */
    fun cleanupOrphanedImages(): Int {
        // This would require database access to check which images are still referenced
        // Implementation should be done in a repository or use case
        return 0
    }
    
    /**
     * Get total storage used by flashcard images.
     * 
     * @return Total size in bytes
     */
    fun getTotalStorageUsed(): Long {
        val imageDir = File(context.filesDir, IMAGE_DIRECTORY)
        if (!imageDir.exists()) return 0
        
        return imageDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
}
