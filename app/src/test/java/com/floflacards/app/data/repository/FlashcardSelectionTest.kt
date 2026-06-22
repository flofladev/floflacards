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

package com.floflacards.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.dao.FlashcardDao
import com.floflacards.app.data.database.FloatingLearningDatabase
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.data.source.BackupPreferences
import com.floflacards.app.domain.usecase.ACTIVE_POOL_CAP
import com.floflacards.app.domain.util.EmptyStateFlashcard
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behavioural tests for the gradual-introduction selection logic in
 * [FlashcardRepository.getNextAvailableFlashcard], exercised against a real in-memory Room DB
 * (so the actual SQL queries run). Covers the active-pool cap, drilling vs. introduction vs.
 * maintenance priority, failed-card handling, empty state, and no-back-to-back-repeat.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlashcardSelectionTest {

    private lateinit var db: FloatingLearningDatabase
    private lateinit var flashcardDao: FlashcardDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var repository: FlashcardRepository
    private var categoryId: Long = 0L

    private val now = System.currentTimeMillis()
    private val past = now - 60_000L      // cooldown elapsed -> due
    private val future = now + 60_000_000L // cooldown not elapsed -> still cooling

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FloatingLearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        flashcardDao = db.flashcardDao()
        categoryDao = db.categoryDao()
        repository = FlashcardRepository(categoryDao, flashcardDao, BackupPreferences(context))
        categoryId = categoryDao.insertCategory(CategoryEntity(name = "Test"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Inserts a card with explicit learning-state fields. Defaults = a brand-new (never-seen) card. */
    private suspend fun insertCard(
        question: String,
        enabled: Boolean = true,
        lastReviewedAt: Long = 0L,
        cooldownUntil: Long = 0L,
        easinessFactor: Float = 2.5f,
        reviewCount: Int = 0,
        createdAt: Long = now,
    ): Long = flashcardDao.insertFlashcard(
        FlashcardEntity(
            categoryId = categoryId,
            question = question,
            answer = "answer",
            isEnabled = enabled,
            lastReviewedAt = lastReviewedAt,
            cooldownUntil = cooldownUntil,
            easinessFactor = easinessFactor,
            reviewCount = reviewCount,
            createdAt = createdAt,
        )
    )

    /** A seen card still being learned (not mastered) that is currently cooling down. */
    private suspend fun insertCoolingLearningCard(question: String) =
        insertCard(question, lastReviewedAt = past, cooldownUntil = future, reviewCount = 1)

    /** A mastered card (EF>=2.5, reviews>=3). */
    private suspend fun insertMasteredCard(question: String, cooldownUntil: Long) =
        insertCard(question, lastReviewedAt = past, cooldownUntil = cooldownUntil, reviewCount = 3)

    private suspend fun draw(): FlashcardEntity = repository.getNextAvailableFlashcard()

    @Test
    fun emptyDeck_returnsEmptyState() = runBlocking {
        assertEquals(EmptyStateFlashcard.EMPTY_STATE_ID, draw().id)
    }

    @Test
    fun onlyDisabledCards_returnsEmptyState() = runBlocking {
        insertCard("disabled", enabled = false)
        assertEquals(EmptyStateFlashcard.EMPTY_STATE_ID, draw().id)
    }

    @Test
    fun fullActivePool_doesNotIntroduceNewCard() = runBlocking {
        // K cards being learned (cooling down) => pool full.
        repeat(ACTIVE_POOL_CAP) { insertCoolingLearningCard("learning-$it") }
        // Plenty of brand-new cards available.
        val newIds = (0 until 5).map { insertCard("new-$it") }.toSet()

        // Nothing is due; pool is full => must fall back to a seen card, never a new one.
        val drawn = draw()
        assertTrue(
            "Expected a seen card, not a newly introduced one",
            drawn.lastReviewedAt > 0 && drawn.id !in newIds
        )
    }

    @Test
    fun freeingASlot_resumesIntroduction() = runBlocking {
        // K-1 learning cards (cooling) + brand-new cards => one free slot.
        repeat(ACTIVE_POOL_CAP - 1) { insertCoolingLearningCard("learning-$it") }
        val newId = insertCard("new")

        val drawn = draw()
        assertEquals("Free slot should introduce the new card", newId, drawn.id)
        assertEquals(0L, drawn.lastReviewedAt) // confirms it was a never-seen card
    }

    @Test
    fun dueLearningCard_preferredOverIntroducingNew() = runBlocking {
        val dueLearningId = insertCard("due-learning", lastReviewedAt = past, cooldownUntil = past, reviewCount = 1)
        insertCard("new-1"); insertCard("new-2")

        assertEquals(dueLearningId, draw().id)
    }

    @Test
    fun failedCard_staysInRotation_notReintroducedAsNew() = runBlocking {
        // A failed card: seen (lastReviewedAt>0) but reviewCount reset to 0 by a WRONG answer.
        val failedId = insertCard("failed", lastReviewedAt = past, cooldownUntil = past, reviewCount = 0)
        insertCard("new")

        val drawn = draw()
        assertEquals("Failed card is due learning, should be drilled", failedId, drawn.id)
        assertTrue("Failed card must count as seen, not new", drawn.lastReviewedAt > 0)
    }

    @Test
    fun newIntroduction_preferredOverMasteredMaintenance() = runBlocking {
        insertMasteredCard("mastered", cooldownUntil = past) // due for maintenance
        val newId = insertCard("new")

        val drawn = draw()
        assertEquals("New learning should outrank maintenance", newId, drawn.id)
    }

    @Test
    fun masteredCard_shownForMaintenanceWhenNothingElse() = runBlocking {
        val masteredId = insertMasteredCard("mastered", cooldownUntil = past)

        val drawn = draw()
        assertEquals(masteredId, drawn.id)
    }

    @Test
    fun newCardsIntroducedOldestFirst() = runBlocking {
        val older = insertCard("older", createdAt = now - 10_000L)
        insertCard("newer", createdAt = now)

        assertEquals(older, draw().id)
    }

    @Test
    fun doesNotShowSameCardTwiceInARow() = runBlocking {
        insertCard("due-1", lastReviewedAt = past, cooldownUntil = past, reviewCount = 1)
        insertCard("due-2", lastReviewedAt = past, cooldownUntil = past, reviewCount = 1)

        val first = draw().id
        val second = draw().id
        assertNotEquals(first, second)
    }

    @Test
    fun singleCardDeck_repeatsTheOnlyCard() = runBlocking {
        val onlyId = insertCard("only", lastReviewedAt = past, cooldownUntil = past, reviewCount = 1)

        assertEquals(onlyId, draw().id)
        assertEquals(onlyId, draw().id) // allowed to repeat when it is the sole card
    }
}
