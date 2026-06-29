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
        // Lifetime count carried over from a restored backup. Kept separate from the id set
        // because restore replaces every card with a fresh id, so old ids can't be matched.
        private const val KEY_LIFETIME_BASELINE = "lifetime_baseline"
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

    /**
     * Number of distinct cards mastered over the app's lifetime: cards mastered in this install
     * (the id set) plus any baseline carried over from a restored backup.
     */
    fun getMasteredCount(): Int {
        val baseline = prefs.getInt(KEY_LIFETIME_BASELINE, 0)
        val thisInstall = prefs.getStringSet(KEY_EVER_MASTERED_IDS, emptySet())?.size ?: 0
        return baseline + thisInstall
    }

    /**
     * Reconciles the lifetime tally after a restore. Restore replaces every card with a brand-new
     * id, so the old id set can't carry over — but a restored card keeps its stats, so the cards
     * that are *currently* mastered can be re-identified by id ([masteredIds], their new ids). We
     * seed the id set with those so re-confirming them later doesn't double-count.
     *
     * The backed-up lifetime total ([backupCount]) is usually larger than the currently-mastered
     * set (it also counts cards that were mastered then later reset/forgotten, which we can't track
     * by id). That remainder is kept in the baseline. The result never decreases relative to what
     * was already here. Call once after a successful restore.
     */
    fun applyRestoredMastered(masteredIds: Set<Long>, backupCount: Int?) {
        // max(...) guarantees a non-negative baseline even if a backup's count is missing/stale.
        val targetLifetime = maxOf(getMasteredCount(), backupCount ?: 0, masteredIds.size)
        val baseline = targetLifetime - masteredIds.size
        prefs.edit()
            .putInt(KEY_LIFETIME_BASELINE, baseline)
            .putStringSet(KEY_EVER_MASTERED_IDS, masteredIds.map { it.toString() }.toSet())
            .apply()
    }
}
