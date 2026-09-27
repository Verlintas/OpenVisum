/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

package verlintas.openvisum.ui.player

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.common.util.LanguageUtils
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.data.Bookmark
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.source.SiblingVideo
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

data class SleepTimerState(
    val minutes: Int? = null,
    val endsAtElapsedMs: Long? = null,
    val endOfItem: Boolean = false,
)

data class PlaylistState(
    val items: List<SiblingVideo> = emptyList(),
    val index: Int = -1,
) {
    val hasPrevious: Boolean get() = index > 0
    val hasNext: Boolean get() = index >= 0 && index < items.size - 1
}

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

    private val _playlist = MutableStateFlow(PlaylistState())
    val playlist: StateFlow<PlaylistState> = _playlist.asStateFlow()

    private val _sleepTimer = MutableStateFlow<SleepTimerState?>(null)
    val sleepTimer: StateFlow<SleepTimerState?> = _sleepTimer.asStateFlow()
    private var sleepJob: Job? = null

    val bookmarks: StateFlow<List<Bookmark>> = engine.state
        .map { it.mediaUri?.toString() }
        .distinctUntilChanged()
        .flatMapLatest { uri ->
            if (uri == null) flowOf(emptyList()) else repository.observeBookmarks(uri)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var openedUri: Uri? = null
    private var lastSavedAt = 0L
    private var lastPosition = -1L
    private var lastPositionChangedAt = 0L
    private var lastRecoveryAt = 0L
    private var rememberPosition = true
    private var autoPlayNext = true
    private var lastEnded = false

    init {
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                rememberPosition = settings.rememberPlaybackPosition
                autoPlayNext = settings.autoPlayNext
            }
        }
        viewModelScope.launch {
            engine.state.collect { playbackState ->
                if (playbackState.isEnded) {
                    if (!lastEnded) {
                        lastEnded = true
                        if (_sleepTimer.value?.endOfItem == true) {
                            clearSleepTimer()
                        } else if (autoPlayNext) {
                            playNext()
                        }
                    }
                } else {
                    lastEnded = false
                }
            }
        }
        viewModelScope.launch {
            engine.state.collect { playbackState ->
                val uri = playbackState.mediaUri ?: return@collect
                if (playbackState.positionMs <= 0L || !rememberPosition) return@collect
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
        viewModelScope.launch {
            while (true) {
                delay(STALL_CHECK_INTERVAL_MS)
                val playbackState = engine.state.value
                if (!playbackState.isPlaying || playbackState.isBuffering || playbackState.durationMs <= 0L) {
                    lastPosition = -1L
                    lastPositionChangedAt = SystemClock.elapsedRealtime()
                    continue
                }
                val now = SystemClock.elapsedRealtime()
                if (playbackState.positionMs != lastPosition) {
                    lastPosition = playbackState.positionMs
                    lastPositionChangedAt = now
                    continue
                }
                val stalledFor = now - lastPositionChangedAt
                if (stalledFor >= STALL_TIMEOUT_MS && now - lastRecoveryAt >= RECOVERY_COOLDOWN_MS) {
                    lastRecoveryAt = now
                    lastPositionChangedAt = now
                    Log.w(TAG, "Playback stalled for ${stalledFor}ms, attempting recovery")
                    runCatching { engine.recoverPlayback() }
                }
            }
        }
    }

    fun openIfNeeded(uri: Uri, title: String?, restart: Boolean = false) {
        if (openedUri == uri && !restart) return
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
            val options = buildList {
                addAll(
                    runCatching { networkRepository.playbackOptionsForUri(uri.toString()) }
                        .getOrDefault(emptyList()),
                )
                if (settings.disableDirectRendering) {
                    add(":mediacodec-dr=0")
                }
            }
            engine.setMedia(uri, resolvedTitle, options)
            val resumePosition = if (settings.rememberPlaybackPosition && !restart) {
                item?.resumePositionMs ?: 0L
            } else {
                0L
            }
            if (resumePosition > 0L) {
                engine.seekTo(resumePosition)
            }
            engine.play()
            if (settings.defaultPlaybackRate != 1.0f) {
                engine.setRate(settings.defaultPlaybackRate)
            }
            if (uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "smb") {
                runCatching { networkRepository.rememberStream(uri.toString(), resolvedTitle) }
            }
            autoLoadExternalSubtitles(
                uri = uri,
                displayName = resolvedTitle,
                settings = settings,
            )
            refreshPlaylist(uri)
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _sleepTimer.value = null
            return
        }
        val durationMs = minutes * 60_000L
        _sleepTimer.value = SleepTimerState(
            minutes = minutes,
            endsAtElapsedMs = SystemClock.elapsedRealtime() + durationMs,
        )
        sleepJob = viewModelScope.launch {
            delay(durationMs)
            engine.pause()
            _sleepTimer.value = null
        }
    }

    fun setSleepTimerEndOfItem() {
        sleepJob?.cancel()
        _sleepTimer.value = SleepTimerState(endOfItem = true)
    }

    fun clearSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimer.value = null
    }

    fun addBookmark() {
        val playbackState = engine.state.value
        val uri = playbackState.mediaUri ?: return
        val position = playbackState.positionMs.coerceAtLeast(0L)
        viewModelScope.launch {
            repository.addBookmark(
                mediaUri = uri.toString(),
                positionMs = position,
                label = TimeUtils.formatDuration(position),
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun selectChapter(index: Int) = engine.setChapter(index)

    fun playNext() {
        val playlist = _playlist.value
        val next = playlist.items.getOrNull(playlist.index + 1) ?: return
        openIfNeeded(next.uri, next.name)
    }

    fun playPrevious() {
        val playlist = _playlist.value
        val previous = playlist.items.getOrNull(playlist.index - 1) ?: return
        openIfNeeded(previous.uri, previous.name)
    }

    private suspend fun refreshPlaylist(uri: Uri) {
        val siblings = repository.findSiblingVideos(uri.toString())
        val index = siblings.indexOfFirst { it.uri == uri }
        _playlist.value = if (index >= 0) {
            PlaylistState(items = siblings, index = index)
        } else {
            PlaylistState()
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
        if (!rememberPosition) return
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

    fun rescanRenderers() {
        engine.stopRendererDiscovery()
        engine.startRendererDiscovery()
    }

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
        private const val SUBTITLE_TRACK_WAIT_MS = 10_000L
        private const val ONLINE_EMPTY_MESSAGE = "No subtitles found"
        private const val ONLINE_ERROR_MESSAGE = "Subtitle search failed"
        private const val STALL_CHECK_INTERVAL_MS = 1_500L
        private const val STALL_TIMEOUT_MS = 6_000L
        private const val RECOVERY_COOLDOWN_MS = 15_000L

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
