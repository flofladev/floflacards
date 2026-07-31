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
import com.floflacards.app.data.csv.CsvParseError
import com.floflacards.app.data.csv.CsvParseResult
import com.floflacards.app.data.csv.CsvParser
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Unit tests for ImportCsvUseCase.
 * Uses fake DAO implementations backed by in-memory lists.
 */
class ImportCsvUseCaseTest {

    private val fakeFlashcardDao = FakeFlashcardDao()
    private val fakeCategoryDao = FakeCategoryDao()
    private val testLogger = object : com.floflacards.app.data.csv.CsvLogger {
        override fun warn(message: String) {}
        override fun error(message: String, throwable: Throwable?) {}
    }

    private lateinit var useCase: ImportCsvUseCase

    @Before
    fun setup() {
        useCase = ImportCsvUseCase(
            csvParser = CsvParser(testLogger),
            flashcardDao = fakeFlashcardDao,
            categoryDao = fakeCategoryDao
        )
    }

    // -- Basic Import --

    @Test
    fun `invoke imports cards from CSV input stream`() = runBlocking {
        val csv = "question,answer\nQ1,A1\nQ2,A2\n"
        val inputStream = csv.toByteArray().inputStream()

        val result = useCase(inputStream, fallbackCategoryId = 1)

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(2, importResult.successCount)
        assertEquals(0, importResult.skippedCount)
        assertEquals(2, fakeFlashcardDao.cards.size)
    }

    @Test
    fun `importFromParsed imports cards without re-reading file`() = runBlocking {
        val csv = "question,answer\nQ1,A1\nQ2,A2\n"
        val parseResult = CsvParser(testLogger).parse(csv.toByteArray().inputStream())

        val result = useCase.importFromParsed(parseResult, fallbackCategoryId = 1)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().successCount)
        assertEquals(2, fakeFlashcardDao.cards.size)
    }

    // -- Duplicate Detection --

    @Test
    fun `skipDuplicates detects duplicates across all categories`() = runBlocking {
        // Pre-populate with a duplicate in a different category
        fakeFlashcardDao.cards.add(
            createEntity(id = 1, categoryId = 2, question = "Q1", answer = "A1")
        )

        val csv = "question,answer\nQ1,A1\nQ2,A2\n"
        val inputStream = csv.toByteArray().inputStream()

        val result = useCase(inputStream, fallbackCategoryId = 1, skipDuplicates = true)

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.successCount) // Only Q2 imported
        assertEquals(1, importResult.skippedCount) // Q1 was duplicate
    }

    @Test
    fun `skipDuplicates false imports duplicates`() = runBlocking {
        fakeFlashcardDao.cards.add(
            createEntity(id = 1, categoryId = 1, question = "Q1", answer = "A1")
        )

        val csv = "question,answer\nQ1,A1\n"
        val inputStream = csv.toByteArray().inputStream()

        val result = useCase(inputStream, fallbackCategoryId = 1, skipDuplicates = false)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().successCount)
        assertEquals(2, fakeFlashcardDao.cards.size) // Original + duplicate
    }

    // -- Category Resolution --

    @Test
    fun `importFromParsed with resolveCategories places cards in matching category`() = runBlocking {
        // Pre-create a category
        fakeCategoryDao.categories.add(CategoryEntity(id = 10, name = "Science"))

        val cards = listOf(
            CsvFlashcard("Q1", "A1", category = "Science"),
            CsvFlashcard("Q2", "A2", category = "Math") // Doesn't exist yet
        )
        val parseResult = CsvParseResult(validCards = cards, errors = emptyList())

        val result = useCase.importFromParsed(
            parseResult,
            fallbackCategoryId = 1,
            resolveCategories = true
        )

        assertTrue(result.isSuccess)
        assertEquals(2, fakeFlashcardDao.cards.size)

        // Q1 should be in Science (existing category, ID=10)
        val scienceCards = fakeFlashcardDao.cards.filter { it.question == "Q1" }
        assertEquals(10L, scienceCards[0].categoryId)

        // Q2 should be in a newly created Math category
        val mathCard = fakeFlashcardDao.cards.find { it.question == "Q2" }
        val mathCategory = fakeCategoryDao.categories.find { it.id == mathCard?.categoryId }
        assertEquals("Math", mathCategory?.name)
    }

    @Test
    fun `importFromParsed without resolveCategories uses fallback category`() = runBlocking {
        val cards = listOf(
            CsvFlashcard("Q1", "A1", category = "Science")
        )
        val parseResult = CsvParseResult(validCards = cards, errors = emptyList())

        val result = useCase.importFromParsed(
            parseResult,
            fallbackCategoryId = 5,
            resolveCategories = false
        )

        assertTrue(result.isSuccess)
        assertEquals(5L, fakeFlashcardDao.cards[0].categoryId)
    }

    @Test
    fun `importFromParsed with null category uses fallback category`() = runBlocking {
        val cards = listOf(
            CsvFlashcard("Q1", "A1", category = null)
        )
        val parseResult = CsvParseResult(validCards = cards, errors = emptyList())

        val result = useCase.importFromParsed(
            parseResult,
            fallbackCategoryId = 42,
            resolveCategories = true
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, fakeFlashcardDao.cards[0].categoryId)
    }

    @Test
    fun `importFromParsed with empty category string uses fallback category`() = runBlocking {
        val cards = listOf(
            CsvFlashcard("Q1", "A1", category = "")
        )
        val parseResult = CsvParseResult(validCards = cards, errors = emptyList())

        val result = useCase.importFromParsed(
            parseResult,
            fallbackCategoryId = 7,
            resolveCategories = true
        )

        assertTrue(result.isSuccess)
        assertEquals(7L, fakeFlashcardDao.cards[0].categoryId)
    }

    @Test
    fun `importFromParsed resolves all 16 categories from the real issue #21 file`() = runBlocking {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csvContent = """
            question,answer,category
            Welche Leinen- und Seilarten nennt die FwDV 1 für den Feuerwehrdienst?,"Feuerwehrleine, Mehrzweckleine, Dynamikseile/Kernmantelseile.",Grundlagen
            Welche Knoten und Stiche sind für die Feuerwehr relevant gemäß FwDV 1?,"Halbschlag, Doppelter Ankerstich, Zimmermannsschlag, Spierenstich, Mastwurf, Achterknoten, Schotenstich, Halbmastwurf, Pfahlstich, Brustbund.",Grundlagen
            Wofür dient der Halbschlag?,"Zum Führen von Gegenständen in Zugrichtung, z. B. beim Hochziehen.",Halbschlag
            Wofür wird der Doppelter Ankerstich verwendet?,Zum Befestigen von Gegenständen und Gerätschaften beim Hochziehen sowie zum Festlegen einer Leine an einem Objekt.,Doppelter Ankerstich
            Wofür dient der Zimmermannsschlag?,"Zum Befestigen von Gegenständen und Material beim Hochziehen, zur Sicherung des Saugkorbs und zum Anbringen einer Sicherungsleine beim Atemschutzeinsatz.",Zimmermannsschlag
            Wofür dient der Spierenstich?,"Zur Sicherung anderer Knoten und Stiche, damit sie sich nicht lösen.",Spierenstich
            Wofür wird der Mastwurf verwendet?,"Zum Befestigen von Gegenständen beim Hochziehen, zum Festlegen an einem Festpunkt, beim Halten, Retten und Selbstretten sowie zum Befestigen des Auszugseils der Schiebleiter.",Mastwurf
            Wofür wird der Achterknoten bei der Feuerwehr verwendet?,"Vorrangig zum Halten, Selbstsichern und Retten, indem die Leine in den Feuerwehr-Haltegurt eingebunden wird.",Achterknoten
            Wofür dient der Schotenstich?,"Zum Verbinden zweier Leinen, besonders wenn sie unterschiedlich stark sind.",Schotenstich
            Wofür wird der Halbmastwurf eingesetzt?,Als Bremsknoten beim Halten und Selbstretten.,Halbmastwurf
            Wofür wird der Pfahlstich verwendet?,"Immer dann, wenn eine Schlaufe gebraucht wird, z. B. beim Brustbund.",Pfahlstich
            Wofür dient der Brustbund?,Zum Einbinden einer Person in eine Leine für die Menschenrettung.,Brustbund
            Welche Knoten werden bei der Rettung einer Person benötigt?,"Brustbund, also Pfahlstich plus Spierenstich.",Rettung
            Welche Knoten eignen sich besonders gut zum Hochziehen von Gegenständen/Geräten?,Doppelter Ankerstich und Achterknoten.,Hochziehen
            Wozu dient der Schotenstich?,"Zum Verbinden zweier Leinen, nicht zur Personensicherung oder Rettung.",Sicherheit
            Welcher Knoten muss grundsätzlich zur Sicherung vor andere Knoten gesetzt werden?,Der Spierenstich.,Sicherheit
            Mit welchem Knoten wird der Brustbund abgeschlossen?,Mit einem festsitzenden Pfahlstich.,Brustbund
            Wie wird das lose Ende beim Brustbund gesichert?,Mit einem doppelten Spierenstich.,Brustbund
            Welche Leinen sollten nicht zur Personensicherung verwendet werden?,"Schlingenstiche, die sich beim Anziehen um den Gegenstand festziehen, sind dafür ungeeignet.",Grundlagen
            Welche Knoten sind beim Sichern einer Schlauchleitung oder Saugleitung wichtig?,Je nach Fall Mastwurf mit Spierenstich oder Zimmermannsschlag – Saugschlauch zusätzlich Halbschlag unterhalb der Kupplung.,Anwendung
            Wofür steht die ABC-Variante?,"A = Axt, B = Beleuchtungsmittel, C = Strahlrohr.",ABC-Variante
            Welche wichtige Faustregel gilt beim Spierenstich?,Er wird immer zur Sicherung anderer Knoten verwendet und darf nicht an der abgehenden Leine befestigt werden.,Spierenstich
            Welche wichtige Faustregel gilt für den Schotenstich bei Personen?,Er darf nicht zur Personensicherung oder Rettung eingesetzt werden.,Sicherheit
            Welche wichtige Faustregel gilt beim Mastwurf an der Feuerwehr-Halteöse?,Er muss gemäß FwDV 1 mit einem Spierenstich gesichert werden.,Mastwurf
            Wie viele Trums/Umschlingungen braucht der Zimmermannsschlag mindestens?,Mindestens 3 vollständige Trums/Umschlingungen.,Zimmermannsschlag
            Was ist der Unterschied zwischen Pfahlstich und Schotenstich?,"Pfahlstich wird als Schlaufe verwendet, Schotenstich zum Verbinden zweier Leinen.",Grundlagen
            Was ist der Zweck des Halbschlags beim Hochziehen?,Er führt den Gegenstand in Zugrichtung.,Halbschlag
            Worauf muss man bei Knoten aus dem losen Ende achten?,"Das lose Ende muss nach dem Festziehen und Sichern noch lang genug überstehen, als Richtwert mindestens 10x Leinendurchmesser.",Grundlagen
        """.trimIndent()

        val csvWithBom = bom + csvContent.toByteArray(Charsets.UTF_8)
        val parseResult = CsvParser(testLogger).parse(csvWithBom.inputStream())

        // Sanity check on parsing itself before asserting on category resolution
        assertTrue(parseResult.errors.isEmpty())
        assertEquals(28, parseResult.validCards.size)

        val result = useCase.importFromParsed(
            parseResult,
            fallbackCategoryId = 999,
            resolveCategories = true
        )

        assertTrue(result.isSuccess)
        assertEquals(28, result.getOrThrow().successCount)

        val expectedCategories = setOf(
            "Grundlagen", "Halbschlag", "Doppelter Ankerstich", "Zimmermannsschlag",
            "Spierenstich", "Mastwurf", "Achterknoten", "Schotenstich", "Halbmastwurf",
            "Pfahlstich", "Brustbund", "Rettung", "Hochziehen", "Sicherheit",
            "Anwendung", "ABC-Variante"
        )
        assertEquals(expectedCategories.size, fakeCategoryDao.categories.size)
        assertEquals(expectedCategories, fakeCategoryDao.categories.map { it.name }.toSet())

        // Every card should be filed under its own resolved category, never the fallback
        fakeFlashcardDao.cards.forEach { card ->
            val category = fakeCategoryDao.categories.find { it.id == card.categoryId }
            assertTrue(category != null && category.name in expectedCategories)
        }
    }

    // -- Error Cases --

    @Test
    fun `importFromParsed fails when no valid cards`() = runBlocking {
        val parseResult = CsvParseResult(
            validCards = emptyList(),
            errors = listOf(CsvParseError(1, "bad", "parse error"))
        )

        val result = useCase.importFromParsed(parseResult, fallbackCategoryId = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `importFromParsed reports all skipped when all are duplicates`() = runBlocking {
        fakeFlashcardDao.cards.add(
            createEntity(id = 1, categoryId = 1, question = "Q1", answer = "A1")
        )

        val csv = "question,answer\nQ1,A1\n"
        val parseResult = CsvParser(testLogger).parse(csv.toByteArray().inputStream())

        val result = useCase.importFromParsed(parseResult, fallbackCategoryId = 1)

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(0, importResult.successCount)
        assertEquals(1, importResult.skippedCount)
        assertEquals(1, fakeFlashcardDao.cards.size) // No new cards
    }

    // -- Helpers --

    private fun createEntity(
        id: Long = 0,
        categoryId: Long,
        question: String,
        answer: String
    ): FlashcardEntity {
        return FlashcardEntity(
            id = id,
            categoryId = categoryId,
            question = question,
            answer = answer,
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

// -- Fake DAOs --

/**
 * In-memory fake of FlashcardDao for unit testing.
 */
class FakeFlashcardDao : FlashcardDao {
    val cards = mutableListOf<FlashcardEntity>()
    private var nextId = 1L

    override suspend fun getExistingQuestionAnswerPairsAllCategories(): List<FlashcardDao.QuestionAnswerPair> {
        return cards.map { FlashcardDao.QuestionAnswerPair(it.question, it.answer) }
    }

    override suspend fun insertFlashcardsBatch(flashcards: List<FlashcardEntity>): List<Long> {
        return flashcards.map { card ->
            val entity = card.copy(id = nextId++)
            cards.add(entity)
            entity.id
        }
    }

    // Unused stubs — Room interfaces require all methods
    override fun getFlashcardsByCategory(categoryId: Long) = throw NotImplementedError()
    override fun getFlashcardCountsPerCategory() = throw NotImplementedError()
    override suspend fun getAllFlashcards() = cards
    override suspend fun getAllFlashcardsForStatistics() = cards
    override suspend fun getFlashcardById(id: Long) = cards.find { it.id == id }
    override suspend fun getNextFlashcardForReview(currentTime: Long, excludeId: Long) = throw NotImplementedError()
    override suspend fun getCardWithShortestCooldown(excludeId: Long) = throw NotImplementedError()
    override suspend fun countActiveLearningCards(masteryEasiness: Float, masteryReviews: Int) = throw NotImplementedError()
    override suspend fun getNextDueLearningCard(currentTime: Long, masteryEasiness: Float, masteryReviews: Int, excludeId: Long) = throw NotImplementedError()
    override suspend fun getNextNewCard(excludeId: Long) = throw NotImplementedError()
    override suspend fun getNextDueMasteredCard(currentTime: Long, masteryEasiness: Float, masteryReviews: Int, excludeId: Long) = throw NotImplementedError()
    override suspend fun getFallbackCard(excludeId: Long) = throw NotImplementedError()
    override suspend fun getNextAvailableFlashcard(currentTime: Long, excludeId: Long) = throw NotImplementedError()
    override suspend fun getActiveFlashcardCount() = cards.size
    override suspend fun getFlashcardCountByCategory(categoryId: Long) =
        cards.count { it.categoryId == categoryId }
    override suspend fun insertFlashcard(flashcard: FlashcardEntity) = nextId++
    override suspend fun updateFlashcard(flashcard: FlashcardEntity) {
        val idx = cards.indexOfFirst { it.id == flashcard.id }
        if (idx >= 0) cards[idx] = flashcard
    }
    override suspend fun deleteFlashcard(flashcard: FlashcardEntity) {
        cards.removeAll { it.id == flashcard.id }
    }
    override suspend fun deleteFlashcardById(id: Long) { cards.removeAll { it.id == id } }
    override suspend fun deleteFlashcardsByCategoryId(categoryId: Long) {
        cards.removeAll { it.categoryId == categoryId }
    }
    override suspend fun resetFlashcardStatistics(flashcardId: Long, timestamp: Long) {}
    override suspend fun resetCategoryStatistics(categoryId: Long, timestamp: Long) {}
    override suspend fun resetAllStatistics(timestamp: Long) {}
    override suspend fun getFlashcardByQuestionAndAnswer(question: String, answer: String) =
        cards.find { it.question == question && it.answer == answer }
    override suspend fun deleteAllFlashcards() { cards.clear() }
    override suspend fun enableAllFlashcardsInCategory(categoryId: Long, timestamp: Long) {}
    override suspend fun disableAllFlashcardsInCategory(categoryId: Long, timestamp: Long) {}
    override suspend fun getEnabledFlashcardCountByCategory(categoryId: Long) =
        cards.count { it.categoryId == categoryId }
    override suspend fun getFlashcardsByCategorySync(categoryId: Long) =
        cards.filter { it.categoryId == categoryId }
}

/**
 * In-memory fake of CategoryDao for unit testing.
 */
class FakeCategoryDao : CategoryDao {
    val categories = mutableListOf<CategoryEntity>()
    private var nextId = 1L

    override suspend fun insertCategory(category: CategoryEntity): Long {
        val id = if (category.id > 0) category.id else nextId++
        categories.add(category.copy(id = id))
        return id
    }

    override suspend fun getAllCategoriesForBackup() = categories
    override suspend fun getCategoryIdByName(name: String): Long? =
        categories.find { it.name.equals(name, ignoreCase = true) }?.id
    override suspend fun getCategoryByName(name: String) =
        categories.find { it.name.equals(name, ignoreCase = true) }

    // Unused stubs
    override fun getAllCategories() = throw NotImplementedError()
    override fun getEnabledCategories() = throw NotImplementedError()
    override suspend fun getCategoryById(id: Long) = categories.find { it.id == id }
    override suspend fun updateCategory(category: CategoryEntity) {
        val idx = categories.indexOfFirst { it.id == category.id }
        if (idx >= 0) categories[idx] = category
    }
    override suspend fun deleteCategory(category: CategoryEntity) {
        categories.removeAll { it.id == category.id }
    }
    override suspend fun deleteCategoryById(id: Long) { categories.removeAll { it.id == id } }
    override suspend fun getCategoryCount() = categories.size
    override suspend fun deleteAllCategories() { categories.clear() }
    override suspend fun enableAllCategories(timestamp: Long) {}
    override suspend fun disableAllCategories(timestamp: Long) {}
    override suspend fun getEnabledCategoryCount() = categories.count { it.isEnabled }
}
