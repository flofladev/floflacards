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

package com.floflacards.app.data.source

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the lifetime "cards mastered with FloFla" achievement.
 *
 * Stores the set of flashcard ids that have EVER been mastered. Using a set means:
 *  - the same card mastered → reset → re-mastered counts once (dedupe),
 *  - the count never decreases when a card is deleted or its stats are reset (ids are never removed),
 * so the home-screen tally is a genuine lifetime achievement, not live (subtractable) progress.
 */
@Singleton
class MasteredCardsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "mastered_cards_preferences"
        private const val KEY_EVER_MASTERED_IDS = "ever_mastered_ids"
    }

    /** Records that [flashcardId] has been mastered. Idempotent; never decreases the count. */
    fun markMastered(flashcardId: Long) {
        val current = prefs.getStringSet(KEY_EVER_MASTERED_IDS, emptySet()) ?: emptySet()
        val id = flashcardId.toString()
        if (id in current) return
        // getStringSet's returned set must not be mutated — write a fresh copy.
        val updated = HashSet(current).apply { add(id) }
        prefs.edit().putStringSet(KEY_EVER_MASTERED_IDS, updated).apply()
    }

    /** Number of distinct cards mastered over the app's lifetime. */
    fun getMasteredCount(): Int =
        prefs.getStringSet(KEY_EVER_MASTERED_IDS, emptySet())?.size ?: 0
}
