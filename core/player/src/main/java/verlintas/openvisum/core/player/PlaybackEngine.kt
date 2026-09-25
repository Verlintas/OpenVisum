package verlintas.openvisum.core.player

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import org.videolan.libvlc.util.DisplayManager
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.player.model.AudioStereoMode
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.SubtitleStyle
import verlintas.openvisum.core.player.model.VideoScaleMode

interface PlaybackEngine {

    val state: StateFlow<PlaybackState>

    fun configureTrackPreferences(
        preferredAudioLanguages: List<String>,
        preferredSubtitleLanguages: List<String>,
    )

    fun setSubtitleStyle(style: SubtitleStyle)

    fun setStereoMode(mode: AudioStereoMode)

    fun attachViews(layout: VLCVideoLayout, displayManager: DisplayManager? = null)

    fun detachViews()

    fun updateVideoSurfaces()

    fun setMedia(uri: Uri, title: String? = null, options: List<String> = emptyList())

    fun play()

    fun pause()

    fun togglePlayPause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun setRate(rate: Float)

    fun selectVideoTrack(trackId: Int)

    fun selectAudioTrack(trackId: Int)

    fun selectSubtitleTrack(trackId: Int)

    fun addSubtitleFile(uri: Uri)

    fun disableSubtitles()

    fun setSubtitleDelay(delayMs: Long)

    fun setAudioDelay(delayMs: Long)

    fun setVolume(volume: Int)

    fun setVideoScale(mode: VideoScaleMode)

    fun setAspectRatio(ratio: String?)

    fun setRotation(degrees: Int)

    fun setEqualizer(config: EqualizerState)

    fun setAudioDigitalOutputEnabled(enabled: Boolean)

    fun setHardwareDecodingEnabled(enabled: Boolean)

    fun setAbLoop(startMs: Long?, endMs: Long?)

    fun release()
}
