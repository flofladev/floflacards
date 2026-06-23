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

package com.floflacards.app.domain.usecase.csv

import com.floflacards.app.data.csv.CsvFlashcard
import com.floflacards.app.data.csv.CsvImportResult
import com.floflacards.app.data.csv.CsvParseResult
import com.floflacards.app.data.csv.CsvParser
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * Use case for importing flashcards from a CSV file.
 *
 * Supports two workflows:
 * 1. Preview: parse CSV and return results for UI preview without DB changes.
 * 2. Import: commit parsed cards to the database with duplicate detection.
 *
 * When cards contain a category name, the use case will attempt to resolve
 * it to an existing category or create a new one. Cards without a category
 * fall back to the user-selected [fallbackCategoryId].
 */
class ImportCsvUseCase @Inject constructor(
    private val csvParser: CsvParser,
    private val flashcardDao: FlashcardDao,
    private val categoryDao: CategoryDao
) {

    /**
     * Parses a CSV file and returns the parse result for preview.
     * Does not modify the database.
     */
    fun parseForPreview(inputStream: InputStream): CsvParseResult {
        return csvParser.parse(inputStream)
    }

    /**
     * Counts how many of the parsed valid cards already exist in the collection (matched by
     * question+answer across all categories — the same scope used when skipping duplicates on
     * import). Lets the UI surface duplicates in the preview before importing.
     */
    suspend fun countExistingDuplicates(parseResult: CsvParseResult): Int = withContext(Dispatchers.IO) {
        if (parseResult.validCards.isEmpty()) return@withContext 0
        val existingPairs = fetchExistingPairs(skipDuplicates = true)
        parseResult.validCards.count { (it.question to it.answer) in existingPairs }
    }

    /**
     * Imports pre-parsed flashcards into the database.
     *
     * This is the preferred method when the user has already previewed the file —
     * it avoids re-reading the InputStream.
     *
     * @param parseResult The result from a previous [parseForPreview] call
     * @param fallbackCategoryId Default category for cards without an explicit category name
     * @param skipDuplicates Whether to skip cards that already exist (matched by question+answer)
     * @param resolveCategories Whether to resolve category names from the CSV into category IDs.
     *   When true, cards with a category field will be placed in a matching category (created if
     *   needed). When false, all cards go into [fallbackCategoryId].
     */
    suspend fun importFromParsed(
        parseResult: CsvParseResult,
        fallbackCategoryId: Long,
        skipDuplicates: Boolean = true,
        resolveCategories: Boolean = false
    ): Result<CsvImportResult> = withContext(Dispatchers.IO) {
        try {
            if (parseResult.validCards.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("No valid flashcards found in CSV file")
                )
            }

            val existingPairs = fetchExistingPairs(skipDuplicates)
            val newCards = filterDuplicates(parseResult.validCards, existingPairs, skipDuplicates)
            val skippedCount = parseResult.validCards.size - newCards.size

            if (newCards.isEmpty()) {
                return@withContext Result.success(
                    CsvImportResult(
                        successCount = 0,
                        skippedCount = skippedCount,
                        errorCount = parseResult.errors.size,
                        errors = listOf("All flashcards are duplicates")
                    )
                )
            }

            // Build category name → ID map (existing categories)
            val categoryCache = if (resolveCategories) {
                categoryDao.getAllCategoriesForBackup()
                    .associateBy({ it.name }, { it.id })
                    .toMutableMap()
            } else {
                mutableMapOf()
            }

            val entities = newCards.map { csvCard ->
                val categoryId = resolveCategoryId(
                    csvCard = csvCard,
                    fallbackCategoryId = fallbackCategoryId,
                    resolveCategories = resolveCategories,
                    categoryCache = categoryCache
                )
                createEntity(csvCard, categoryId)
            }

            val insertedIds = flashcardDao.insertFlashcardsBatch(entities)
            val successCount = insertedIds.count { it > 0 }

            Result.success(
                CsvImportResult(
                    successCount = successCount,
                    skippedCount = skippedCount,
                    errorCount = parseResult.errors.size,
                    errors = emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports flashcards from a CSV InputStream.
     * Parses the file and immediately commits to the database.
     */
    suspend operator fun invoke(
        inputStream: InputStream,
        fallbackCategoryId: Long,
        skipDuplicates: Boolean = true,
        resolveCategories: Boolean = false
    ): Result<CsvImportResult> = withContext(Dispatchers.IO) {
        try {
            val parseResult = csvParser.parse(inputStream)
            performImport(parseResult, fallbackCategoryId, skipDuplicates, resolveCategories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -- Internal Helpers --

    private suspend fun performImport(
        parseResult: CsvParseResult,
        fallbackCategoryId: Long,
        skipDuplicates: Boolean,
        resolveCategories: Boolean
    ): Result<CsvImportResult> {
        if (parseResult.validCards.isEmpty()) {
            return Result.failure(
                IllegalStateException("No valid flashcards found in CSV file")
            )
        }

        val existingPairs = fetchExistingPairs(skipDuplicates)
        val newCards = filterDuplicates(parseResult.validCards, existingPairs, skipDuplicates)
        val skippedCount = parseResult.validCards.size - newCards.size

        if (newCards.isEmpty()) {
            return Result.success(
                CsvImportResult(
                    successCount = 0,
                    skippedCount = skippedCount,
                    errorCount = parseResult.errors.size,
                    errors = listOf("All flashcards are duplicates")
                )
            )
        }

        val categoryCache = mutableMapOf<String, Long>()

        val entities = newCards.map { csvCard ->
            val categoryId = resolveCategoryId(
                csvCard = csvCard,
                fallbackCategoryId = fallbackCategoryId,
                resolveCategories = resolveCategories,
                categoryCache = categoryCache
            )
            createEntity(csvCard, categoryId)
        }

        val insertedIds = flashcardDao.insertFlashcardsBatch(entities)
        val successCount = insertedIds.count { it > 0 }

        return Result.success(
            CsvImportResult(
                successCount = successCount,
                skippedCount = skippedCount,
                errorCount = parseResult.errors.size,
                errors = emptyList()
            )
        )
    }

    /**
     * Fetches existing question+answer pairs across ALL categories for duplicate detection.
     * This prevents importing the same card twice regardless of target category.
     */
    private suspend fun fetchExistingPairs(
        skipDuplicates: Boolean
    ): Set<Pair<String, String>> {
        return if (skipDuplicates) {
            flashcardDao.getExistingQuestionAnswerPairsAllCategories()
                .map { it.question to it.answer }
                .toSet()
        } else {
            emptySet()
        }
    }

    private fun filterDuplicates(
        cards: List<CsvFlashcard>,
        existingPairs: Set<Pair<String, String>>,
        skipDuplicates: Boolean
    ): List<CsvFlashcard> {
        return if (skipDuplicates) {
            cards.filter { card ->
                val pair = card.question to card.answer
                pair !in existingPairs
            }
        } else {
            cards
        }
    }

    /**
     * Resolves the target category ID for a card.
     *
     * If [resolveCategories] is true and the card has a category name:
     *   - Looks up the category by name in [categoryCache]
     *   - If not found, creates a new category and caches it
     *
     * Otherwise, returns [fallbackCategoryId].
     */
    private suspend fun resolveCategoryId(
        csvCard: CsvFlashcard,
        fallbackCategoryId: Long,
        resolveCategories: Boolean,
        categoryCache: MutableMap<String, Long>
    ): Long {
        val categoryName = csvCard.category?.takeIf { it.isNotBlank() }

        if (!resolveCategories || categoryName.isNullOrBlank()) {
            return fallbackCategoryId
        }

        // Check cache first
        categoryCache[categoryName]?.let { return it }

        // Try to find existing category by name (case-insensitive)
        val existingId = categoryDao.getCategoryIdByName(categoryName)
        if (existingId != null) {
            categoryCache[categoryName] = existingId
            return existingId
        }

        // Create new category
        val newCategory = CategoryEntity(name = categoryName)
        val newId = categoryDao.insertCategory(newCategory)
        categoryCache[categoryName] = newId
        return newId
    }

    private fun createEntity(csvCard: CsvFlashcard, categoryId: Long): FlashcardEntity {
        return FlashcardEntity(
            categoryId = categoryId,
            question = csvCard.question,
            answer = csvCard.answer,
            questionImagePath = null,
            answerImagePath = null,
            isEnabled = true,
            correctCount = 0,
            incorrectCount = 0,
            hardCount = 0,
            easinessFactor = 2.5f,
            reviewCount = 0,
            lastReviewedAt = 0,
            cooldownUntil = 0
        )
    }
}
