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

package verlintas.openvisum.core.player

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import org.videolan.libvlc.util.DisplayManager
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.core.player.model.AudioStereoMode
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.RendererDevice
import verlintas.openvisum.core.player.model.SubtitleStyle
import verlintas.openvisum.core.player.model.VideoScaleMode

interface PlaybackEngine {

    val state: StateFlow<PlaybackState>

    val renderers: StateFlow<List<RendererDevice>>

    val activeRenderer: StateFlow<RendererDevice?>

    fun startRendererDiscovery()

    fun stopRendererDiscovery()

    fun connectRenderer(deviceId: String)

    fun disconnectRenderer()

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

    fun recoverPlayback()

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

    fun setChapter(index: Int)

    fun nextChapter()

    fun previousChapter()

    fun setRotation(degrees: Int)

    fun setEqualizer(config: EqualizerState)

    fun setAudioDigitalOutputEnabled(enabled: Boolean)

    fun setHardwareDecodingEnabled(enabled: Boolean)

    fun setAbLoop(startMs: Long?, endMs: Long?)

    fun release()
}
