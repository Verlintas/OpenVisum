package verlintas.openvisum.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.DisplayManager
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.PlayerTrack
import verlintas.openvisum.core.player.model.TrackType
import verlintas.openvisum.core.player.model.VideoScaleMode

class VlcPlaybackEngine(context: Context) : PlaybackEngine {

    private val appContext = context.applicationContext
    private val libVlc = LibVLC(appContext, ArrayList(DEFAULT_OPTIONS))
    private val mediaPlayer = MediaPlayer(libVlc)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var currentMedia: Media? = null
    private var hwDecodingEnabled = true
    private var rotationDegrees = 0
    private var externalSubtitleUri: Uri? = null
    private var abLoopStartMs: Long? = null
    private var abLoopEndMs: Long? = null
    private var restoring = false
    private var released = false

    init {
        _state.update { it.copy(passthroughAvailable = mediaPlayer.canDoPassthrough()) }
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening -> _state.update { it.copy(isBuffering = true, isEnded = false) }
                MediaPlayer.Event.Playing -> _state.update {
                    it.copy(
                        isPlaying = true,
                        isBuffering = false,
                        isEnded = false,
                        passthroughAvailable = mediaPlayer.canDoPassthrough(),
                    )
                }

                MediaPlayer.Event.Paused -> _state.update { it.copy(isPlaying = false) }
                MediaPlayer.Event.Stopped -> _state.update { it.copy(isPlaying = false) }
                MediaPlayer.Event.EndReached -> _state.update { it.copy(isPlaying = false, isEnded = true) }
                MediaPlayer.Event.Buffering -> {
                    val fraction = event.buffering / 100f
                    _state.update { it.copy(isBuffering = fraction < 1f, bufferedFraction = fraction) }
                }

                MediaPlayer.Event.TimeChanged -> onTimeChanged(event.timeChanged)
                MediaPlayer.Event.LengthChanged -> _state.update { it.copy(durationMs = event.lengthChanged) }
                MediaPlayer.Event.SeekableChanged -> _state.update { it.copy(seekable = event.seekable) }
                MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.ESSelected -> {
                    refreshTracks()
                    refreshVideoSize()
                }

                MediaPlayer.Event.Vout -> refreshVideoSize()
                MediaPlayer.Event.EncounteredError -> handlePlaybackError()
            }
        }
    }

    override fun attachViews(layout: VLCVideoLayout, displayManager: DisplayManager?) {
        if (!mediaPlayer.vlcVout.areViewsAttached()) {
            mediaPlayer.attachViews(layout, displayManager, true, false)
        }
    }

    override fun detachViews() {
        if (mediaPlayer.vlcVout.areViewsAttached()) {
            mediaPlayer.detachViews()
        }
    }

    override fun updateVideoSurfaces() {
        if (mediaPlayer.vlcVout.areViewsAttached()) {
            mediaPlayer.updateVideoSurfaces()
        }
    }

    override fun setMedia(uri: Uri, title: String?, options: List<String>) {
        check(!released) { "Engine already released" }
        externalSubtitleUri = null
        openMedia(uri, title, options, positionMs = 0L)
    }

    private fun openMedia(uri: Uri, title: String?, options: List<String>, positionMs: Long) {
        releaseMedia()
        val media = Media(libVlc, uri).apply {
            setHWDecoderEnabled(hwDecodingEnabled, false)
            addOption(":sub-autodetect-file=false")
            if (rotationDegrees != 0) addOption(":video-rotate=$rotationDegrees")
            options.forEach { addOption(it) }
        }
        currentMedia = media
        mediaPlayer.media = media
        _state.update {
            it.copy(
                mediaUri = uri,
                title = title ?: uri.lastPathSegment,
                positionMs = positionMs,
                isEnded = false,
                errorMessage = null,
                videoTracks = emptyList(),
                audioTracks = emptyList(),
                subtitleTracks = emptyList(),
            )
        }
        if (positionMs > 0L) {
            pendingSeekMs = positionMs
        }
    }

    private var pendingSeekMs: Long? = null

    override fun play() {
        if (_state.value.isEnded) {
            mediaPlayer.setTime(0L)
            _state.update { it.copy(positionMs = 0L, isEnded = false) }
        }
        applyPendingSeekIfNeeded()
        mediaPlayer.play()
    }

    override fun pause() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    override fun togglePlayPause() {
        if (mediaPlayer.isPlaying) pause() else play()
    }

    override fun stop() {
        mediaPlayer.stop()
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    override fun seekTo(positionMs: Long) {
        if (_state.value.durationMs <= 0L) {
            pendingSeekMs = positionMs
            return
        }
        mediaPlayer.setTime(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L), isEnded = false) }
    }

    private fun applyPendingSeekIfNeeded() {
        val pending = pendingSeekMs ?: return
        pendingSeekMs = null
        mediaPlayer.setTime(pending)
    }

    override fun setRate(rate: Float) {
        val coerced = rate.coerceIn(0.25f, 4f)
        mediaPlayer.setRate(coerced)
        _state.update { it.copy(rate = coerced) }
    }

    override fun selectVideoTrack(trackId: Int) {
        mediaPlayer.setVideoTrack(trackId)
        refreshTracks()
    }

    override fun selectAudioTrack(trackId: Int) {
        mediaPlayer.setAudioTrack(trackId)
        refreshTracks()
    }

    override fun selectSubtitleTrack(trackId: Int) {
        if (trackId == PlayerTrack.TRACK_DISABLED) {
            disableSubtitles()
        } else {
            mediaPlayer.setSpuTrack(trackId)
            refreshTracks()
        }
    }

    override fun addSubtitleFile(uri: Uri) {
        externalSubtitleUri = uri
        val added = mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, uri, true)
        if (!added) {
            _state.update { it.copy(errorMessage = "Failed to load subtitle: ${uri.lastPathSegment}") }
        } else {
            refreshTracks()
        }
    }

    override fun disableSubtitles() {
        mediaPlayer.setSpuTrack(PlayerTrack.TRACK_DISABLED)
        _state.update { it.copy(selectedSubtitleTrackId = PlayerTrack.TRACK_DISABLED) }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        mediaPlayer.setSpuDelay(delayMs * 1000L)
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun setAudioDelay(delayMs: Long) {
        mediaPlayer.setAudioDelay(delayMs * 1000L)
        _state.update { it.copy(audioDelayMs = delayMs) }
    }

    override fun setVolume(volume: Int) {
        mediaPlayer.setVolume(volume.coerceIn(0, 100))
    }

    override fun setVideoScale(mode: VideoScaleMode) {
        val scaleType = when (mode) {
            VideoScaleMode.FIT_SCREEN -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
            VideoScaleMode.FILL_SCREEN -> MediaPlayer.ScaleType.SURFACE_FILL
            VideoScaleMode.ORIGINAL -> MediaPlayer.ScaleType.SURFACE_ORIGINAL
            VideoScaleMode.RATIO_16_9 -> MediaPlayer.ScaleType.SURFACE_16_9
            VideoScaleMode.RATIO_4_3 -> MediaPlayer.ScaleType.SURFACE_4_3
            VideoScaleMode.RATIO_21_9 -> MediaPlayer.ScaleType.SURFACE_2_1
            VideoScaleMode.RATIO_235_1 -> MediaPlayer.ScaleType.SURFACE_235_1
        }
        mediaPlayer.setVideoScale(scaleType)
        _state.update { it.copy(videoScale = mode, aspectRatio = null) }
    }

    override fun setAspectRatio(ratio: String?) {
        mediaPlayer.setAspectRatio(ratio)
        _state.update { it.copy(aspectRatio = ratio) }
    }

    override fun setRotation(degrees: Int) {
        val normalized = ((degrees % 360) + 360) % 360
        rotationDegrees = normalized
        _state.update { it.copy(rotationDegrees = normalized) }
        reloadPreservingState()
    }

    override fun setEqualizer(config: EqualizerState) {
        if (!config.enabled) {
            mediaPlayer.setEqualizer(null)
            _state.update { it.copy(equalizer = config.copy(enabled = false)) }
            return
        }
        val equalizer = MediaPlayer.Equalizer.create().apply {
            setPreAmp(config.preamp)
            config.bandAmplitudes.forEachIndexed { index, amplitude -> setAmp(index, amplitude) }
        }
        mediaPlayer.setEqualizer(equalizer)
        _state.update { it.copy(equalizer = config) }
    }

    override fun setAudioDigitalOutputEnabled(enabled: Boolean) {
        val success = mediaPlayer.setAudioDigitalOutputEnabled(enabled)
        _state.update { it.copy(audioDigitalOutput = enabled && success) }
    }

    override fun setHardwareDecodingEnabled(enabled: Boolean) {
        if (hwDecodingEnabled == enabled) return
        hwDecodingEnabled = enabled
        reloadPreservingState()
    }

    override fun setAbLoop(startMs: Long?, endMs: Long?) {
        abLoopStartMs = startMs
        abLoopEndMs = endMs
    }

    private fun onTimeChanged(positionMs: Long) {
        val loopStart = abLoopStartMs
        val loopEnd = abLoopEndMs
        if (loopStart != null && loopEnd != null && loopEnd > loopStart && positionMs >= loopEnd) {
            mediaPlayer.setTime(loopStart)
            _state.update { it.copy(positionMs = loopStart) }
            return
        }
        _state.update { it.copy(positionMs = positionMs) }
    }

    private fun refreshTracks() {
        val media = mediaPlayer.media
        val details = mutableMapOf<Int, IMedia.Track>()
        if (media != null && media.isParsed) {
            for (index in 0 until media.trackCount) {
                val track = runCatching { media.getTrack(index) }.getOrNull() ?: continue
                details[track.id] = track
            }
        }
        val videoTracks = mediaPlayer.videoTracks.orEmpty().map { description ->
            toPlayerTrack(TrackType.VIDEO, description.id, description.name, details)
        }
        val audioTracks = mediaPlayer.audioTracks.orEmpty().map { description ->
            toPlayerTrack(TrackType.AUDIO, description.id, description.name, details)
        }
        val subtitleTracks = mediaPlayer.spuTracks.orEmpty().map { description ->
            toPlayerTrack(TrackType.SUBTITLE, description.id, description.name, details)
        }
        _state.update {
            it.copy(
                videoTracks = videoTracks,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                selectedVideoTrackId = mediaPlayer.videoTrack,
                selectedAudioTrackId = mediaPlayer.audioTrack,
                selectedSubtitleTrackId = mediaPlayer.spuTrack,
            )
        }
    }

    private fun toPlayerTrack(
        type: TrackType,
        id: Int,
        fallbackName: String,
        details: Map<Int, IMedia.Track>,
    ): PlayerTrack {
        val detail = details[id]
        val name = detail?.description?.takeIf { it.isNotBlank() } ?: fallbackName
        val external = detail?.let { it.fourcc == 0 && it.bitrate == 0 && id < 0 } ?: false
        return when (detail) {
            is IMedia.AudioTrack -> PlayerTrack(
                id = id,
                type = type,
                name = name,
                language = detail.language,
                codec = detail.codec,
                channels = detail.channels,
                sampleRate = detail.rate,
                external = external,
            )

            is IMedia.VideoTrack -> PlayerTrack(
                id = id,
                type = type,
                name = name,
                language = detail.language,
                codec = detail.codec,
                width = detail.width,
                height = detail.height,
                frameRate = if (detail.frameRateDen > 0) {
                    detail.frameRateNum.toFloat() / detail.frameRateDen
                } else {
                    null
                },
                external = external,
            )

            is IMedia.SubtitleTrack -> PlayerTrack(
                id = id,
                type = type,
                name = name,
                language = detail.language,
                codec = detail.codec,
                external = external,
            )

            else -> PlayerTrack(
                id = id,
                type = type,
                name = name,
                language = detail?.language,
                codec = detail?.codec,
            )
        }
    }

    private fun refreshVideoSize() {
        val videoTrack = mediaPlayer.currentVideoTrack ?: return
        val effectiveRotation = if (videoTrack.orientation != 0) videoTrack.orientation else rotationDegrees
        val swapped = effectiveRotation == 90 || effectiveRotation == 270
        val width = if (swapped) videoTrack.height else videoTrack.width
        val height = if (swapped) videoTrack.width else videoTrack.height
        if (width > 0 && height > 0) {
            _state.update { it.copy(videoWidth = width, videoHeight = height) }
        }
    }

    private fun handlePlaybackError() {
        Log.w(TAG, "Playback error (hardware decoding=$hwDecodingEnabled)")
        if (hwDecodingEnabled) {
            hwDecodingEnabled = false
            val uri = _state.value.mediaUri
            if (uri != null) {
                val position = _state.value.positionMs
                openMedia(uri, _state.value.title, emptyList(), position)
                restoring = true
                play()
                restoring = false
                return
            }
        }
        _state.update {
            it.copy(isPlaying = false, isBuffering = false, errorMessage = it.title ?: "Playback failed")
        }
    }

    private fun reloadPreservingState() {
        if (restoring) return
        val uri = _state.value.mediaUri ?: return
        val position = _state.value.positionMs
        val selectedAudio = _state.value.selectedAudioTrackId
        val selectedSubtitle = _state.value.selectedSubtitleTrackId
        val subtitleUri = externalSubtitleUri
        val wasPlaying = mediaPlayer.isPlaying
        restoring = true
        try {
            openMedia(uri, _state.value.title, emptyList(), position)
            mediaPlayer.play()
            if (selectedAudio >= 0) mediaPlayer.setAudioTrack(selectedAudio)
            if (selectedSubtitle >= 0) mediaPlayer.setSpuTrack(selectedSubtitle)
            subtitleUri?.let { mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, it, true) }
            if (!wasPlaying) mediaPlayer.pause()
        } finally {
            restoring = false
        }
    }

    private fun releaseMedia() {
        mediaPlayer.stop()
        currentMedia?.let {
            mediaPlayer.media = null
            it.release()
        }
        currentMedia = null
    }

    override fun release() {
        if (released) return
        released = true
        runCatching {
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            releaseMedia()
            mediaPlayer.release()
            libVlc.release()
        }
    }

    companion object {
        private const val TAG = "VlcPlaybackEngine"

        private val DEFAULT_OPTIONS = listOf(
            "--audio-time-stretch",
            "--file-caching=1000",
            "--network-caching=3000",
            "--live-caching=3000",
            "--no-snapshot-preview",
        )
    }
}
