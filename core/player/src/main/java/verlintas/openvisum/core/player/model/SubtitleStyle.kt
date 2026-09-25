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
