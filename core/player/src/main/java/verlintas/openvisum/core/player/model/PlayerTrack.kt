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

enum class TrackType { VIDEO, AUDIO, SUBTITLE }

data class PlayerTrack(
    val id: Int,
    val type: TrackType,
    val name: String,
    val language: String? = null,
    val codec: String? = null,
    val channels: Int? = null,
    val sampleRate: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Float? = null,
    val external: Boolean = false,
) {
    val isDisabled: Boolean get() = id == TRACK_DISABLED

    companion object {
        const val TRACK_DISABLED = -1
    }
}
