package verlintas.openvisum.ui.player

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.prefs.PreferencesRepository
import verlintas.openvisum.core.player.PlaybackEngine
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.VideoScaleMode

class PlayerViewModel(
    private val engine: PlaybackEngine,
    private val repository: MediaRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<PlaybackState> = engine.state

    private var openedUri: Uri? = null
    private var lastSavedAt = 0L

    init {
        viewModelScope.launch {
            engine.state.collect { playbackState ->
                val uri = playbackState.mediaUri ?: return@collect
                if (playbackState.positionMs <= 0L) return@collect
                val now = SystemClock.elapsedRealtime()
                val shouldSave = now - lastSavedAt >= SAVE_INTERVAL_MS ||
                    (!playbackState.isPlaying && playbackState.positionMs != playbackState.durationMs)
                if (shouldSave) {
                    lastSavedAt = now
                    repository.saveProgress(
                        uri = uri.toString(),
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs,
                    )
                }
            }
        }
    }

    fun openIfNeeded(uri: Uri, title: String?) {
        if (openedUri == uri) return
        openedUri = uri
        viewModelScope.launch {
            val settings = preferences.settings.first()
            engine.setHardwareDecodingEnabled(settings.hardwareDecoding)
            engine.setMedia(uri, title)
            val item = runCatching { repository.find(uri.toString()) }.getOrNull()
            val resumePosition = item?.resumePositionMs ?: 0L
            if (resumePosition > 0L) {
                engine.seekTo(resumePosition)
            }
            engine.play()
        }
    }

    fun saveProgressNow() {
        val playbackState = engine.state.value
        val uri = playbackState.mediaUri ?: return
        if (playbackState.positionMs <= 0L) return
        viewModelScope.launch {
            repository.saveProgress(
                uri = uri.toString(),
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
            )
        }
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
        private const val SAVE_INTERVAL_MS = 5_000L

        fun factory(
            engine: PlaybackEngine,
            repository: MediaRepository,
            preferences: PreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(engine, repository, preferences) as T
            }
        }
    }
}
