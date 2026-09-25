package verlintas.openvisum.ui.player

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.common.util.LanguageUtils
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.NetworkRepository
import verlintas.openvisum.core.data.prefs.AppSettings
import verlintas.openvisum.core.data.prefs.PreferencesRepository
import verlintas.openvisum.core.data.source.SubtitleFile
import verlintas.openvisum.core.data.subtitle.SubtitleSearchRepository
import verlintas.openvisum.core.data.subtitle.SubtitleSearchResult
import verlintas.openvisum.core.player.PlaybackEngine
import verlintas.openvisum.core.player.model.AudioStereoMode
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.RendererDevice
import verlintas.openvisum.core.player.model.SubtitleStyle
import verlintas.openvisum.core.player.model.VideoScaleMode

data class OnlineSubtitleState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SubtitleSearchResult> = emptyList(),
    val downloadingId: String? = null,
    val message: String? = null,
)

class PlayerViewModel(
    private val engine: PlaybackEngine,
    private val repository: MediaRepository,
    private val networkRepository: NetworkRepository,
    private val subtitleSearchRepository: SubtitleSearchRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<PlaybackState> = engine.state

    val renderers: StateFlow<List<RendererDevice>> = engine.renderers

    val activeRenderer: StateFlow<RendererDevice?> = engine.activeRenderer

    private val _onlineSubtitles = MutableStateFlow(OnlineSubtitleState())
    val onlineSubtitles: StateFlow<OnlineSubtitleState> = _onlineSubtitles.asStateFlow()

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
            engine.setSubtitleStyle(settings.toSubtitleStyle())
            engine.setStereoMode(AudioStereoMode.fromValue(settings.stereoMode))
            engine.configureTrackPreferences(
                preferredAudioLanguages = settings.preferredAudioLanguages,
                preferredSubtitleLanguages = settings.preferredSubtitleLanguages,
            )
            val item = runCatching { repository.find(uri.toString()) }.getOrNull()
            val resolvedTitle = title ?: item?.displayName ?: repository.resolveDisplayName(uri)
            val options = runCatching { networkRepository.playbackOptionsForUri(uri.toString()) }
                .getOrDefault(emptyList())
            engine.setMedia(uri, resolvedTitle, options)
            val resumePosition = item?.resumePositionMs ?: 0L
            if (resumePosition > 0L) {
                engine.seekTo(resumePosition)
            }
            engine.play()
            if (uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "smb") {
                runCatching { networkRepository.rememberStream(uri.toString(), resolvedTitle) }
            }
            autoLoadExternalSubtitles(
                uri = uri,
                displayName = resolvedTitle,
                settings = settings,
            )
        }
    }

    fun searchOnlineSubtitles(query: String) {
        _onlineSubtitles.update {
            it.copy(query = query, isSearching = true, results = emptyList(), message = null)
        }
        viewModelScope.launch {
            val settings = preferences.settings.first()
            val languages = openSubtitlesLanguages(settings.preferredSubtitleLanguages)
            runCatching { subtitleSearchRepository.search(query, languages) }
                .onSuccess { results ->
                    _onlineSubtitles.update {
                        it.copy(
                            isSearching = false,
                            results = results,
                            message = if (results.isEmpty()) ONLINE_EMPTY_MESSAGE else null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _onlineSubtitles.update {
                        it.copy(
                            isSearching = false,
                            message = throwable.message ?: ONLINE_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    fun downloadSubtitle(result: SubtitleSearchResult) {
        _onlineSubtitles.update { it.copy(downloadingId = result.id, message = null) }
        viewModelScope.launch {
            subtitleSearchRepository.download(result)
                .onSuccess { file ->
                    val known = engine.state.value.subtitleTracks.mapTo(mutableSetOf()) { it.id }
                    engine.addSubtitleFile(Uri.fromFile(file))
                    waitForNewSubtitleTrack(known)?.let(engine::selectSubtitleTrack)
                    _onlineSubtitles.update { it.copy(downloadingId = null, message = null) }
                }
                .onFailure { throwable ->
                    _onlineSubtitles.update {
                        it.copy(
                            downloadingId = null,
                            message = throwable.message ?: ONLINE_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    fun dismissOnlineSubtitleMessage() {
        _onlineSubtitles.update { it.copy(message = null) }
    }

    private suspend fun autoLoadExternalSubtitles(
        uri: Uri,
        displayName: String?,
        settings: AppSettings,
    ) {
        if (!settings.autoLoadExternalSubtitles) return
        val files = repository.findSiblingSubtitles(uri.toString(), displayName)
        if (files.isEmpty()) {
            Log.i(TAG, "No sibling subtitles found for $displayName ($uri)")
            return
        }
        Log.i(TAG, "External subtitles found: ${files.map { "${it.name}(${it.language})" }}")

        val trackIds = mutableMapOf<SubtitleFile, Int>()
        files.take(MAX_EXTERNAL_SUBTITLES).forEach { file ->
            val known = engine.state.value.subtitleTracks.mapTo(mutableSetOf()) { it.id }
            engine.addSubtitleFile(file.uri)
            waitForNewSubtitleTrack(known)?.let { trackId -> trackIds[file] = trackId }
        }
        Log.i(TAG, "External subtitle tracks: ${trackIds.mapValues { it.value }}")

        val best = pickBestSubtitle(files, settings.preferredSubtitleLanguages) ?: return
        val bestTrackId = trackIds[best] ?: return
        Log.i(TAG, "Auto-selected subtitle: ${best.name} (track $bestTrackId)")
        engine.selectSubtitleTrack(bestTrackId)
    }

    private suspend fun waitForNewSubtitleTrack(knownIds: Set<Int>): Int? {
        val deadline = SystemClock.elapsedRealtime() + SUBTITLE_TRACK_WAIT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val newTrack = engine.state.value.subtitleTracks.firstOrNull { track ->
                track.id >= 0 && track.id !in knownIds
            }
            if (newTrack != null) return newTrack.id
            delay(150L)
        }
        return null
    }

    private fun pickBestSubtitle(
        files: List<SubtitleFile>,
        preferredLanguages: List<String>,
    ): SubtitleFile? {
        for (language in preferredLanguages) {
            val match = files.firstOrNull { file ->
                LanguageUtils.matches(listOf(language), file.language)
            }
            if (match != null) return match
        }
        return files.singleOrNull()
    }

    private fun openSubtitlesLanguages(languages: List<String>): String =
        languages.mapNotNull { language ->
            when (LanguageUtils.normalize(language)) {
                "zh-Hans" -> "zh-cn"
                "zh-Hant" -> "zh-tw"
                "zh" -> "zh-cn"
                null -> null
                else -> LanguageUtils.normalize(language)
            }
        }.distinct().joinToString(",").ifBlank { "en" }

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

    fun markAbLoopStart() {
        val playbackState = engine.state.value
        engine.setAbLoop(playbackState.positionMs, playbackState.abLoopEndMs)
    }

    fun markAbLoopEnd() {
        val playbackState = engine.state.value
        val start = playbackState.abLoopStartMs ?: return
        val end = playbackState.positionMs
        if (end > start) engine.setAbLoop(start, end)
    }

    fun clearAbLoop() = engine.setAbLoop(null, null)

    fun startRendererDiscovery() = engine.startRendererDiscovery()

    fun stopRendererDiscovery() = engine.stopRendererDiscovery()

    fun connectRenderer(deviceId: String) = engine.connectRenderer(deviceId)

    fun disconnectRenderer() = engine.disconnectRenderer()

    fun setSubtitleStyle(style: SubtitleStyle) {
        engine.setSubtitleStyle(style)
        viewModelScope.launch {
            preferences.setSubtitleStyle(style.textScale, style.bold, style.color)
        }
    }

    fun setStereoMode(mode: AudioStereoMode) {
        engine.setStereoMode(mode)
        viewModelScope.launch { preferences.setStereoMode(mode.vlcValue) }
    }

    override fun onCleared() {
        engine.stop()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
        private const val SAVE_INTERVAL_MS = 5_000L
        private const val MAX_EXTERNAL_SUBTITLES = 5
        private const val SUBTITLE_TRACK_WAIT_MS = 6_000L
        private const val ONLINE_EMPTY_MESSAGE = "No subtitles found"
        private const val ONLINE_ERROR_MESSAGE = "Subtitle search failed"

        fun factory(
            engine: PlaybackEngine,
            repository: MediaRepository,
            networkRepository: NetworkRepository,
            subtitleSearchRepository: SubtitleSearchRepository,
            preferences: PreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(
                    engine,
                    repository,
                    networkRepository,
                    subtitleSearchRepository,
                    preferences,
                ) as T
            }
        }
    }
}

fun AppSettings.toSubtitleStyle(): SubtitleStyle = SubtitleStyle(
    textScale = subtitleScale,
    bold = subtitleBold,
    color = subtitleColor,
)
