package verlintas.openvisum.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.player.PlaybackEngine
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.VideoScaleMode

class PlayerViewModel(private val engine: PlaybackEngine) : ViewModel() {

    val state: StateFlow<PlaybackState> = engine.state

    private var openedUri: Uri? = null

    fun openIfNeeded(uri: Uri, title: String?) {
        if (openedUri == uri) return
        openedUri = uri
        engine.setMedia(uri, title)
        engine.play()
    }

    fun attach(layout: VLCVideoLayout) = engine.attachViews(layout)

    fun detach() = engine.detachViews()

    fun onSurfaceLayoutChanged() = engine.updateVideoSurfaces()

    fun togglePlayPause() = engine.togglePlayPause()

    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)

    fun seekBy(deltaMs: Long) = engine.seekTo(engine.state.value.positionMs + deltaMs)

    fun setRate(rate: Float) = engine.setRate(rate)

    fun selectVideoTrack(trackId: Int) = engine.selectVideoTrack(trackId)

    fun selectAudioTrack(trackId: Int) = engine.selectAudioTrack(trackId)

    fun selectSubtitleTrack(trackId: Int) = engine.selectSubtitleTrack(trackId)

    fun addSubtitleFile(uri: Uri) = engine.addSubtitleFile(uri)

    fun disableSubtitles() = engine.disableSubtitles()

    fun setSubtitleDelay(delayMs: Long) = engine.setSubtitleDelay(delayMs)

    fun setAudioDelay(delayMs: Long) = engine.setAudioDelay(delayMs)

    fun setVolume(volume: Int) = engine.setVolume(volume)

    fun setVideoScale(mode: VideoScaleMode) = engine.setVideoScale(mode)

    fun setRotation(degrees: Int) = engine.setRotation(degrees)

    fun setEqualizer(config: EqualizerState) = engine.setEqualizer(config)

    fun setAudioDigitalOutput(enabled: Boolean) = engine.setAudioDigitalOutputEnabled(enabled)

    override fun onCleared() {
        engine.stop()
    }

    companion object {
        fun factory(engine: PlaybackEngine): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(engine) as T
                }
            }
    }
}
