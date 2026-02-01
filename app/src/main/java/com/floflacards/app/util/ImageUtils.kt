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

package com.floflacards.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility object for image compression and resizing operations.
 * Follows single responsibility principle - only handles image transformations.
 */
object ImageUtils {
    
    private const val MAX_IMAGE_DIMENSION = 2048 // Maximum width or height in pixels
    private const val JPEG_QUALITY = 85 // Quality percentage for JPEG compression
    private const val THUMBNAIL_SIZE = 100 // Thumbnail size in pixels
    
    /**
     * Compress and resize an image from URI.
     * Returns the compressed bitmap or null if operation fails.
     * 
     * @param context Application context
     * @param uri Source image URI
     * @return Compressed bitmap or null
     */
    fun compressImage(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // Rotate if needed based on EXIF orientation
            val rotatedBitmap = rotateImageIfRequired(context, uri, originalBitmap)
            
            // Resize if too large
            resizeBitmap(rotatedBitmap, MAX_IMAGE_DIMENSION)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Create a thumbnail from an image file.
     * 
     * @param imagePath Path to the source image
     * @return Thumbnail bitmap or null
     */
    fun createThumbnail(imagePath: String): Bitmap? {
        return try {
            val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: return null
            resizeBitmap(originalBitmap, THUMBNAIL_SIZE)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save bitmap to file with JPEG compression.
     * 
     * @param bitmap Bitmap to save
     * @param file Target file
     * @return True if successful, false otherwise
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Resize bitmap maintaining aspect ratio.
     * 
     * @param bitmap Original bitmap
     * @param maxSize Maximum dimension (width or height)
     * @return Resized bitmap
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // No need to resize if already smaller
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        // Calculate new dimensions maintaining aspect ratio
        val scale = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Rotate image based on EXIF orientation data.
     * Some cameras save images rotated and store orientation in EXIF.
     * 
     * @param context Application context
     * @param uri Image URI
     * @param bitmap Original bitmap
     * @return Rotated bitmap if needed, original otherwise
     */
    private fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Rotate bitmap by specified degrees.
     * 
     * @param bitmap Original bitmap
     * @param degrees Rotation angle in degrees
     * @return Rotated bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
