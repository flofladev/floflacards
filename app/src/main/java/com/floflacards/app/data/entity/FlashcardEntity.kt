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

package com.floflacards.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val question: String,
    val answer: String,
    val questionImagePath: String? = null,
    val answerImagePath: String? = null,
    val isEnabled: Boolean = true,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val hardCount: Int = 0, // Number of times "Hard" button was pressed
    // SM-2 Algorithm fields
    val easinessFactor: Float = 2.5f, // SM-2 easiness factor (1.3 - 2.5)
    val reviewCount: Int = 0, // Number of successful reviews
    val lastReviewedAt: Long = 0,
    val cooldownUntil: Long = 0, // Timestamp when card becomes available again
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
