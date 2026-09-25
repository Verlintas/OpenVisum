package verlintas.openvisum.core.common.util

import java.util.Locale

data class SubtitleMatch(
    val fileName: String,
    val score: Int,
    val language: String?,
    val forced: Boolean,
)

object SubtitleMatcher {

    val SUBTITLE_EXTENSIONS = setOf(
        "srt", "ass", "ssa", "vtt", "sub", "smi", "sami", "ttml", "dfxp", "mpl", "txt",
    )

    private val LANGUAGE_TOKENS = setOf(
        "zh", "zho", "chi", "chs", "cht", "zh-cn", "zh-tw", "zh-hans", "zh-hant",
        "en", "eng", "english", "chinese", "jpn", "jp", "jap", "japanese",
        "kor", "kr", "korean", "ger", "deu", "german", "fre", "fra", "french",
        "spa", "esp", "spanish", "por", "portuguese", "rus", "russian",
        "ita", "italian", "tha", "thai", "vie", "vietnamese", "ara", "arabic",
        "hin", "hindi", "chs&eng", "cht&eng", "zh-en", "zh&en", "bilingual",
        "简体", "繁体", "中文", "双语", "中英", "英字", "简中", "繁中",
    )

    private val RELEASE_TOKENS = setOf(
        "1080p", "720p", "2160p", "4k", "8k", "hdr", "sdr", "dv", "hdr10",
        "bluray", "blu-ray", "brrip", "bdrip", "webrip", "web-dl", "webdl", "hdtv",
        "dvdrip", "hdrip", "remux", "x264", "x265", "h264", "h265", "hevc", "avc",
        "aac", "ac3", "eac3", "dts", "truehd", "atmos", "flac", "ddp", "dd5", "ddp5",
        "10bit", "8bit", "repack", "proper", "extended", "imax", "uhd", "multi",
    )

    const val SCORE_EXACT = 100
    const val SCORE_BASE_WITH_LANGUAGE = 90
    const val SCORE_CONTAINS_BASE = 60
    const val SCORE_FUZZY = 30

    fun isSubtitleFile(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in SUBTITLE_EXTENSIONS

    fun baseName(fileName: String): String = fileName.substringBeforeLast('.')

    fun findMatches(
        videoFileName: String,
        subtitleFileNames: List<String>,
        forcedSuffixes: Set<String> = setOf("forced"),
    ): List<SubtitleMatch> {
        val videoBase = baseName(videoFileName)
        val videoKey = normalizeKey(videoBase)
        if (videoKey.isEmpty()) return emptyList()
        val videoTokens = significantTokens(videoBase)

        return subtitleFileNames
            .filter { isSubtitleFile(it) && !it.equals(videoFileName, ignoreCase = true) }
            .mapNotNull { subName ->
                val subBase = baseName(subName)
                val subKey = normalizeKey(subBase)
                if (subKey.isEmpty()) return@mapNotNull null
                val subTokens = significantTokens(subBase)

                val languageToken = extractLanguageToken(subBase, videoBase)
                val forced = extractForced(subBase, videoBase)
                val score = when {
                    subBase.equals(videoBase, ignoreCase = true) -> SCORE_EXACT
                    extensionMatch(subBase, videoBase) != null -> SCORE_BASE_WITH_LANGUAGE
                    subTokens == videoTokens -> SCORE_BASE_WITH_LANGUAGE
                    containsTokens(subTokens, videoTokens) || containsTokens(videoTokens, subTokens) ->
                        SCORE_CONTAINS_BASE

                    prefixOverlapRatio(videoTokens, subTokens) >= 0.6 -> SCORE_FUZZY
                    similarity(subKey, videoKey) >= 0.85 -> SCORE_FUZZY
                    else -> return@mapNotNull null
                }
                SubtitleMatch(
                    fileName = subName,
                    score = score + if (languageToken != null) 5 else 0,
                    language = languageToken,
                    forced = forced,
                )
            }
            .sortedWith(compareByDescending<SubtitleMatch> { it.score }.thenBy { it.fileName })
    }

    fun extractLanguageToken(subBase: String, videoBase: String): String? {
        val suffix = extensionMatch(subBase, videoBase) ?: subBase
        val tokens = suffix.split('.', '-', '_', ' ', '[', ']', '(', ')')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
        for (token in tokens) {
            if (token in LANGUAGE_TOKENS) return token
        }
        return null
    }

    private fun extractForced(subBase: String, videoBase: String): Boolean {
        val suffix = extensionMatch(subBase, videoBase) ?: subBase
        return suffix.lowercase(Locale.ROOT).contains("forced")
    }

    private fun extensionMatch(subBase: String, videoBase: String): String? {
        if (!subBase.startsWith(videoBase, ignoreCase = true)) return null
        val suffix = subBase.substring(videoBase.length)
        if (suffix.isEmpty()) return null
        val first = suffix.first()
        return if (first == '.' || first == '-' || first == '_' || first == ' ') suffix else null
    }

    private fun normalizeKey(value: String): String = significantTokens(value).joinToString("")

    private fun significantTokens(value: String): List<String> {
        val rawTokens = value.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        return rawTokens
            .filterNot { it.lowercase(Locale.ROOT) in RELEASE_TOKENS }
            .filterNot { isNoiseToken(it) }
            .map { it.lowercase(Locale.ROOT) }
    }

    private fun isNoiseToken(rawToken: String): Boolean {
        if (rawToken.all { it.isDigit() }) return rawToken.length <= 2
        if (rawToken.length in 2..5 && rawToken.all { it.isUpperCase() || it.isDigit() }) return true
        val token = rawToken.lowercase(Locale.ROOT)
        return token.matches(Regex("^(x|h)?26[45]$")) || token.matches(Regex("^\\d{3,4}p$"))
    }

    private fun containsTokens(haystack: List<String>, needle: List<String>): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }

    private fun prefixOverlapRatio(a: List<String>, b: List<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var overlap = 0
        while (overlap < a.size && overlap < b.size && a[overlap] == b[overlap]) {
            overlap++
        }
        return overlap.toDouble() / minOf(a.size, b.size)
    }

    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val distance = levenshtein(a, b)
        return 1.0 - distance.toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
