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

package verlintas.openvisum.dlna

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import verlintas.openvisum.MainActivity
import verlintas.openvisum.core.player.PlaybackEngine
import verlintas.openvisum.core.player.receiver.RendererHost

/**
 * Binds the DLNA renderer to the application: remote transport commands drive
 * the shared playback engine, and incoming media brings the player screen up.
 */
class DlnaRendererHost(
    private val context: Context,
    private val engine: PlaybackEngine,
) : RendererHost {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var volume = DEFAULT_VOLUME

    @Volatile
    private var muted = false

    @Volatile
    private var volumeBeforeMute = DEFAULT_VOLUME

    @Volatile
    private var remoteStopped = false

    override fun deviceName(): String = "OpenVisum · ${Build.MODEL}"

    override fun onRemoteOpen(uri: String, metadata: String?) {
        remoteStopped = false
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            action = Intent.ACTION_VIEW
            setDataAndType(Uri.parse(uri), "*/*")
        }
        runCatching { context.startActivity(intent) }
    }

    override fun onRemotePlay() {
        remoteStopped = false
        mainHandler.post { engine.play() }
    }

    override fun onRemotePause() {
        mainHandler.post { engine.pause() }
    }

    override fun onRemoteStop() {
        remoteStopped = true
        mainHandler.post { engine.stop() }
    }

    override fun onRemoteSeek(positionMs: Long) {
        mainHandler.post { engine.seekTo(positionMs) }
    }

    override fun onRemoteSetVolume(percent: Int) {
        val value = percent.coerceIn(0, 100)
        volume = value
        volumeBeforeMute = value
        mainHandler.post { engine.setVolume(if (muted) 0 else value) }
    }

    override fun onRemoteSetMute(muted: Boolean) {
        this.muted = muted
        val target = if (muted) 0 else volumeBeforeMute.takeIf { it > 0 } ?: volume
        mainHandler.post { engine.setVolume(target) }
    }

    override fun hasMedia(): Boolean = engine.state.value.mediaUri != null

    override fun isPlaying(): Boolean = engine.state.value.isPlaying

    override fun isEnded(): Boolean = remoteStopped || engine.state.value.isEnded

    override fun positionMs(): Long = engine.state.value.positionMs

    override fun durationMs(): Long = engine.state.value.durationMs

    override fun currentUri(): String? = engine.state.value.mediaUri?.toString()

    override fun volumePercent(): Int = volume

    override fun isMuted(): Boolean = muted

    private companion object {
        const val DEFAULT_VOLUME = 100
    }
}
