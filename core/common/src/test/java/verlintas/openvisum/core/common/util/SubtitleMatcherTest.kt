package verlintas.openvisum.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleMatcherTest {

    @Test
    fun matchesExactBaseName() {
        val matches = SubtitleMatcher.findMatches(
            videoFileName = "Inception.2010.1080p.BluRay.x264.mkv",
            subtitleFileNames = listOf("Inception.2010.1080p.BluRay.x264.srt"),
        )
        assertEquals(1, matches.size)
        assertEquals(SubtitleMatcher.SCORE_EXACT, matches.first().score)
    }

    @Test
    fun extractsLanguageFromSuffix() {
        val matches = SubtitleMatcher.findMatches(
            videoFileName = "Movie.2024.mkv",
            subtitleFileNames = listOf("Movie.2024.zh.srt", "Movie.2024.eng.ass", "Movie.2024.chs&eng.ass"),
        )
        assertEquals(3, matches.size)
        assertEquals("zh", matches.first { it.fileName == "Movie.2024.zh.srt" }.language)
        assertEquals("eng", matches.first { it.fileName == "Movie.2024.eng.ass" }.language)
        assertEquals("chs&eng", matches.first { it.fileName == "Movie.2024.chs&eng.ass" }.language)
    }

    @Test
    fun exactMatchRanksAboveLanguageSuffix() {
        val matches = SubtitleMatcher.findMatches(
            videoFileName = "Movie.2024.mkv",
            subtitleFileNames = listOf("Movie.2024.srt", "Movie.2024.zh.srt"),
        )
        assertEquals("Movie.2024.srt", matches.first().fileName)
        assertEquals(SubtitleMatcher.SCORE_EXACT, matches.first().score)
        val chinese = matches.first { it.fileName == "Movie.2024.zh.srt" }
        assertEquals("zh", chinese.language)
        assertTrue(chinese.score > 0)
    }

    @Test
    fun ignoresReleaseTagsWhenFuzzyMatching() {
        val matches = SubtitleMatcher.findMatches(
            videoFileName = "Movie.2024.2160p.WEB-DL.DDP5.1.HDR.x265.mkv",
            subtitleFileNames = listOf("Movie.2024.WEB-DL.zh-Hans.srt"),
        )
        assertEquals(1, matches.size)
        assertEquals("zh", matches.first().language)
    }

    @Test
    fun detectsForcedSubtitles() {
        val matches = SubtitleMatcher.findMatches(
            videoFileName = "Movie.2024.mkv",
            subtitleFileNames = listOf("Movie.2024.eng.forced.srt"),
        )
        assertTrue(matches.first().forced)
    }

    @Test
    fun rejectsUnrelatedSubtitles() {
        val matches = SubtitleMatcher.findMatches(
            videoFileName = "Movie.2024.mkv",
            subtitleFileNames = listOf("AnotherMovie.srt", "notes.txt"),
        )
        assertTrue(
            matches.joinToString { "${it.fileName}:${it.score}" },
            matches.isEmpty(),
        )
    }

    @Test
    fun languageNormalization() {
        assertEquals("zh-Hans", LanguageUtils.normalize("chs"))
        assertEquals("zh-Hant", LanguageUtils.normalize("cht"))
        assertEquals("en", LanguageUtils.normalize("ENG"))
        assertEquals("ja", LanguageUtils.normalize("jpn"))
        assertNull(LanguageUtils.normalize(""))
        assertTrue(LanguageUtils.matches(listOf("zh"), "chi"))
    }
}
