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

package com.floflacards.app.data.model

/**
 * How noticeable the entrance-cue edge glow is. One knob covers both alpha and
 * strip width on purpose: "too light to see" in peripheral vision is as much a
 * size problem as a brightness problem, so the presets scale them together.
 *
 * @param peakAlpha alpha of the gradient seam at the very screen edge
 * @param midAlpha alpha of the gradient's middle stop before it fades out
 * @param stripWidthDp width of the glow strip window
 */
enum class GlowIntensity(
    val peakAlpha: Float,
    val midAlpha: Float,
    val stripWidthDp: Float
) {
    SUBTLE(0.4f, 0.12f, 24f),
    NORMAL(0.65f, 0.2f, 32f),
    STRONG(0.9f, 0.3f, 44f);

    companion object {
        val DEFAULT = NORMAL

        fun fromString(value: String?): GlowIntensity =
            entries.find { it.name == value } ?: DEFAULT
    }
}
