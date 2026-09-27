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

package verlintas.openvisum.core.player

import verlintas.openvisum.core.common.util.LanguageUtils
import verlintas.openvisum.core.player.model.PlayerTrack

object TrackSelector {

    fun selectAudio(
        tracks: List<PlayerTrack>,
        preferredLanguages: List<String>,
    ): PlayerTrack? {
        val candidates = tracks.filter { it.id >= 0 }
        if (candidates.isEmpty()) return null
        val preferred = preferredLanguages.mapNotNull { LanguageUtils.normalize(it) }
        val scored = candidates.map { track -> track to score(track, preferred) }
        val best = scored.filter { it.second > 0 }.maxByOrNull { it.second }
        if (best != null) return best.first
        return candidates.firstOrNull { !it.language.isNullOrBlank() } ?: candidates.first()
    }

    fun selectSubtitle(
        tracks: List<PlayerTrack>,
        preferredLanguages: List<String>,
    ): PlayerTrack? {
        val candidates = tracks.filter { it.id >= 0 }
        if (candidates.isEmpty() || preferredLanguages.isEmpty()) return null
        val preferred = preferredLanguages.mapNotNull { LanguageUtils.normalize(it) }
        return candidates
            .map { track -> track to score(track, preferred) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun score(track: PlayerTrack, preferred: List<String>): Int {
        val language = LanguageUtils.normalize(track.language) ?: return 0
        val index = preferred.indexOfFirst { pref ->
            language == pref || language.startsWith("$pref-") || pref.startsWith("$language-")
        }
        if (index < 0) return 0
        return 1000 - index
    }
}
