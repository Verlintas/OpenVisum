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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import verlintas.openvisum.core.player.model.PlayerTrack
import verlintas.openvisum.core.player.model.TrackType

class TrackSelectorTest {

    private fun audio(id: Int, language: String?) = PlayerTrack(
        id = id,
        type = TrackType.AUDIO,
        name = "Track $id",
        language = language,
    )

    private fun subtitle(id: Int, language: String?) = PlayerTrack(
        id = id,
        type = TrackType.SUBTITLE,
        name = "Sub $id",
        language = language,
    )

    @Test
    fun prefersLanguageOrder() {
        val tracks = listOf(audio(1, "en"), audio(2, "ja"), audio(3, "zh"))
        val selected = TrackSelector.selectAudio(tracks, listOf("zh", "en"))
        assertEquals(3, selected?.id)
    }

    @Test
    fun matchesLanguageVariants() {
        val tracks = listOf(audio(1, "en"), audio(2, "zh-Hant"))
        assertEquals(2, TrackSelector.selectAudio(tracks, listOf("zh"))?.id)
    }

    @Test
    fun fallsBackToFirstTrackWithLanguage() {
        val tracks = listOf(audio(1, null), audio(2, "de"))
        assertEquals(2, TrackSelector.selectAudio(tracks, listOf("zh"))?.id)
    }

    @Test
    fun fallsBackToFirstTrack() {
        val tracks = listOf(audio(1, null), audio(2, null))
        assertEquals(1, TrackSelector.selectAudio(tracks, listOf("zh"))?.id)
    }

    @Test
    fun subtitleOnlySelectedOnLanguageMatch() {
        val tracks = listOf(subtitle(1, "en"), subtitle(2, "zh-Hans"))
        assertEquals(2, TrackSelector.selectSubtitle(tracks, listOf("zh"))?.id)
        assertNull(TrackSelector.selectSubtitle(tracks, listOf("fr")))
    }

    @Test
    fun ignoresDisabledTrack() {
        val tracks = listOf(audio(-1, "zh"), audio(2, "en"))
        assertEquals(2, TrackSelector.selectAudio(tracks, listOf("zh"))?.id)
    }
}
