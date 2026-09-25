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
