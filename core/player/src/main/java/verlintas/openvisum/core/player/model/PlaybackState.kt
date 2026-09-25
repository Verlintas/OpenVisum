package verlintas.openvisum.core.player.model

import android.net.Uri

data class PlaybackState(
    val mediaUri: Uri? = null,
    val title: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedFraction: Float = 0f,
    val rate: Float = 1f,
    val seekable: Boolean = true,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val rotationDegrees: Int = 0,
    val videoTracks: List<PlayerTrack> = emptyList(),
    val audioTracks: List<PlayerTrack> = emptyList(),
    val subtitleTracks: List<PlayerTrack> = emptyList(),
    val selectedVideoTrackId: Int = 0,
    val selectedAudioTrackId: Int = 0,
    val selectedSubtitleTrackId: Int = PlayerTrack.TRACK_DISABLED,
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val videoScale: VideoScaleMode = VideoScaleMode.FIT_SCREEN,
    val aspectRatio: String? = null,
    val equalizer: EqualizerState = EqualizerState(),
    val audioDigitalOutput: Boolean = false,
    val passthroughAvailable: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasVideo: Boolean get() = videoTracks.isNotEmpty() || videoWidth > 0
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

enum class VideoScaleMode {
    FIT_SCREEN,
    FILL_SCREEN,
    ORIGINAL,
    RATIO_16_9,
    RATIO_4_3,
    RATIO_21_9,
    RATIO_235_1,
}

data class EqualizerState(
    val enabled: Boolean = false,
    val presetIndex: Int = 0,
    val presetName: String? = null,
    val preamp: Float = 0f,
    val bandAmplitudes: List<Float> = emptyList(),
)
