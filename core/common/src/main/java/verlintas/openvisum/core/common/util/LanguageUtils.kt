package verlintas.openvisum.core.common.util

import java.util.Locale

object LanguageUtils {

    private val SPECIAL_ALIASES = mapOf(
        "zh" to "zh",
        "zho" to "zh",
        "chi" to "zh",
        "chs" to "zh-Hans",
        "cht" to "zh-Hant",
        "zh-cn" to "zh-Hans",
        "zh-hans" to "zh-Hans",
        "zh-sg" to "zh-Hans",
        "chi-simplified" to "zh-Hans",
        "zh-tw" to "zh-Hant",
        "zh-hk" to "zh-Hant",
        "zh-hant" to "zh-Hant",
        "chi-traditional" to "zh-Hant",
        "eng" to "en",
        "en-us" to "en",
        "en-gb" to "en",
        "jpn" to "ja",
        "jp" to "ja",
        "jap" to "ja",
        "kor" to "ko",
        "kr" to "ko",
        "ger" to "de",
        "deu" to "de",
        "fre" to "fr",
        "fra" to "fr",
        "spa" to "es",
        "esp" to "es",
        "por" to "pt",
        "pt-br" to "pt",
        "rus" to "ru",
        "ita" to "it",
        "tha" to "th",
        "vie" to "vi",
        "ara" to "ar",
        "hin" to "hi",
        "und" to "",
    )

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().lowercase(Locale.ROOT).replace('_', '-')
        SPECIAL_ALIASES[cleaned]?.let { return it.ifEmpty { null } }
        val language = cleaned.substringBefore('-')
        SPECIAL_ALIASES[language]?.let { return it.ifEmpty { null } }
        return if (language.length in 2..3 && language.all { it in 'a'..'z' }) language else null
    }

    fun displayName(raw: String?): String? {
        val normalized = normalize(raw) ?: return null
        val locale = Locale.forLanguageTag(normalized)
        val name = locale.getDisplayLanguage(Locale.getDefault())
        return if (name.isBlank()) normalized else name.replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }

    fun matches(preferred: List<String>, trackLanguage: String?): Boolean {
        val normalizedTrack = normalize(trackLanguage) ?: return false
        return preferred.any { pref ->
            val normalizedPref = normalize(pref) ?: return@any false
            normalizedTrack == normalizedPref ||
                normalizedTrack.startsWith("$normalizedPref-") ||
                normalizedPref.startsWith("$normalizedTrack-")
        }
    }
}
