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
