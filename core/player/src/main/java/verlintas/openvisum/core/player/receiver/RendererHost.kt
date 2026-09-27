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

package verlintas.openvisum.core.player.receiver

/**
 * Bridge between the UPnP renderer services and the application.
 *
 * All methods are called from jUPnP threads; implementations must marshal
 * calls to the appropriate (main) thread themselves.
 */
interface RendererHost {

    /** Friendly name advertised over SSDP. */
    fun deviceName(): String

    /** A controller asked to load media. The app should open the player. */
    fun onRemoteOpen(uri: String, metadata: String?)

    fun onRemotePlay()

    fun onRemotePause()

    fun onRemoteStop()

    fun onRemoteSeek(positionMs: Long)

    fun onRemoteSetVolume(percent: Int)

    fun onRemoteSetMute(muted: Boolean)

    fun hasMedia(): Boolean

    fun isPlaying(): Boolean

    fun isEnded(): Boolean

    fun positionMs(): Long

    fun durationMs(): Long

    fun currentUri(): String?

    /** 0..100 */
    fun volumePercent(): Int

    fun isMuted(): Boolean
}
