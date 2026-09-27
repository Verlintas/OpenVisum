/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

package verlintas.openvisum.core.player.model

data class SubtitleStyle(
    val textScale: Float = 1.0f,
    val bold: Boolean = false,
    val color: Int? = null,
) {
    val isDefault: Boolean
        get() = textScale == DEFAULT_SCALE && !bold && color == null

    companion object {
        const val DEFAULT_SCALE = 1.0f
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 2.5f

        val COLORS: List<Pair<String, Int>> = listOf(
            "default" to 0xFFFFFF,
            "white" to 0xFFFFFF,
            "yellow" to 0xFFFF00,
            "cyan" to 0x00FFFF,
            "green" to 0x00FF00,
        )
    }
}

enum class AudioStereoMode(val vlcValue: Int) {
    AUTO(0),
    STEREO(1),
    REVERSE_STEREO(2),
    LEFT(3),
    RIGHT(4),
    DOLBY(5),
    ;

    companion object {
        fun fromValue(value: Int): AudioStereoMode =
            entries.firstOrNull { it.vlcValue == value } ?: AUTO
    }
}
