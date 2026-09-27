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

import java.net.URI
import java.util.Locale
import org.jupnp.model.types.ErrorCode
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.support.avtransport.AVTransportErrorCode
import org.jupnp.support.avtransport.AVTransportException
import org.jupnp.support.avtransport.AbstractAVTransportService
import org.jupnp.support.avtransport.lastchange.AVTransportVariable
import org.jupnp.support.lastchange.LastChange
import org.jupnp.support.model.DeviceCapabilities
import org.jupnp.support.model.MediaInfo
import org.jupnp.support.model.PlayMode
import org.jupnp.support.model.PositionInfo
import org.jupnp.support.model.RecordQualityMode
import org.jupnp.support.model.StorageMedium
import org.jupnp.support.model.TransportAction
import org.jupnp.support.model.TransportInfo
import org.jupnp.support.model.TransportSettings
import org.jupnp.support.model.TransportState

class RendererAvTransportService(
    lastChange: LastChange,
    private val host: RendererHost,
    private val onStateChanged: () -> Unit = {},
) : AbstractAVTransportService(lastChange) {

    @Volatile
    private var currentUri: String? = null

    @Volatile
    private var currentMetadata: String? = null

    override fun getCurrentInstanceIds(): Array<UnsignedIntegerFourBytes> =
        arrayOf(AbstractAVTransportService.getDefaultInstanceID())

    override fun setAVTransportURI(
        instanceId: UnsignedIntegerFourBytes,
        currentURI: String?,
        currentURIMetaData: String?,
    ) {
        val uri = currentURI ?: throw AVTransportException(ErrorCode.INVALID_ARGS, "CurrentURI can not be null")
        if (!isSupportedUri(uri)) {
            throw AVTransportException(
                AVTransportErrorCode.PLAYBACK_FORMAT_NOT_SUPPORTED,
                "Unsupported URI scheme: $uri",
            )
        }
        currentUri = uri
        currentMetadata = currentURIMetaData
        host.onRemoteOpen(uri, currentURIMetaData)
        publishState(TransportState.STOPPED)
    }

    override fun setNextAVTransportURI(
        instanceId: UnsignedIntegerFourBytes,
        nextURI: String?,
        nextURIMetaData: String?,
    ) {
        throw AVTransportException(ErrorCode.OPTIONAL_ACTION, "Next URI is not supported")
    }

    override fun getMediaInfo(instanceId: UnsignedIntegerFourBytes): MediaInfo = MediaInfo(
        currentUri ?: "",
        currentMetadata ?: "",
        UnsignedIntegerFourBytes(1),
        formatTime(host.durationMs()),
        StorageMedium.NETWORK,
    )

    override fun getTransportInfo(instanceId: UnsignedIntegerFourBytes): TransportInfo =
        TransportInfo(transportState())

    override fun getPositionInfo(instanceId: UnsignedIntegerFourBytes): PositionInfo {
        val position = host.positionMs().coerceAtLeast(0L)
        return PositionInfo(
            1L,
            formatTime(host.durationMs()),
            currentMetadata ?: "",
            currentUri ?: "",
            formatTime(position),
            formatTime(position),
            0,
            0,
        )
    }

    override fun getDeviceCapabilities(instanceId: UnsignedIntegerFourBytes): DeviceCapabilities =
        DeviceCapabilities(
            arrayOf(StorageMedium.NETWORK),
            arrayOf(StorageMedium.NETWORK),
            arrayOf(RecordQualityMode.NOT_IMPLEMENTED),
        )

    override fun getTransportSettings(instanceId: UnsignedIntegerFourBytes): TransportSettings =
        TransportSettings(PlayMode.NORMAL, RecordQualityMode.NOT_IMPLEMENTED)

    override fun stop(instanceId: UnsignedIntegerFourBytes) {
        host.onRemoteStop()
        publishState(TransportState.STOPPED)
    }

    override fun play(instanceId: UnsignedIntegerFourBytes, speed: String?) {
        if (currentUri == null && !host.hasMedia()) {
            throw AVTransportException(AVTransportErrorCode.NO_CONTENTS, "No media loaded")
        }
        host.onRemotePlay()
        publishState(TransportState.PLAYING)
    }

    override fun pause(instanceId: UnsignedIntegerFourBytes) {
        host.onRemotePause()
        publishState(TransportState.PAUSED_PLAYBACK)
    }

    override fun record(instanceId: UnsignedIntegerFourBytes) {
        throw AVTransportException(ErrorCode.OPTIONAL_ACTION, "Recording is not supported")
    }

    override fun seek(instanceId: UnsignedIntegerFourBytes, unit: String?, target: String?) {
        if (currentUri == null && !host.hasMedia()) {
            throw AVTransportException(AVTransportErrorCode.NO_CONTENTS, "No media loaded")
        }
        val mode = SeekUnit.fromUnit(unit.orEmpty())
        if (mode != SeekUnit.REL_TIME) {
            throw AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek unit: $unit")
        }
        val positionMs = parseTime(target)
        if (positionMs < 0L) {
            throw AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET, "Invalid seek target: $target")
        }
        host.onRemoteSeek(positionMs)
        publishState(transportState())
    }

    override fun next(instanceId: UnsignedIntegerFourBytes) {
        throw AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE, "No next media")
    }

    override fun previous(instanceId: UnsignedIntegerFourBytes) {
        throw AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE, "No previous media")
    }

    override fun setPlayMode(instanceId: UnsignedIntegerFourBytes, newPlayMode: String?) {
        val mode = runCatching {
            PlayMode.valueOf((newPlayMode ?: "").uppercase(Locale.ROOT))
        }.getOrNull()
        if (mode != PlayMode.NORMAL) {
            throw AVTransportException(AVTransportErrorCode.PLAYMODE_NOT_SUPPORTED, "Only NORMAL play mode is supported")
        }
    }

    override fun setRecordQualityMode(instanceId: UnsignedIntegerFourBytes, newRecordQualityMode: String?) {
        throw AVTransportException(ErrorCode.OPTIONAL_ACTION, "Recording is not supported")
    }

    protected override fun getCurrentTransportActions(instanceId: UnsignedIntegerFourBytes): Array<TransportAction> {
        if (!host.hasMedia()) {
            return arrayOf()
        }
        return arrayOf(TransportAction.Play, TransportAction.Pause, TransportAction.Stop, TransportAction.Seek)
    }

    private fun transportState(): TransportState {
        if (!host.hasMedia()) return TransportState.NO_MEDIA_PRESENT
        if (host.isPlaying()) return TransportState.PLAYING
        if (host.isEnded()) return TransportState.STOPPED
        return if (host.positionMs() > 0L) TransportState.PAUSED_PLAYBACK else TransportState.STOPPED
    }

    private fun publishState(state: TransportState) {
        runCatching {
            val lastChange = getLastChange() ?: return
            lastChange.setEventedValue(
                AbstractAVTransportService.getDefaultInstanceID(),
                AVTransportVariable.TransportState(state),
            )
            currentUri?.let { uri ->
                runCatching {
                    lastChange.setEventedValue(
                        AbstractAVTransportService.getDefaultInstanceID(),
                        AVTransportVariable.CurrentTrackURI(URI(uri)),
                    )
                }
            }
            onStateChanged()
        }
    }

    private fun isSupportedUri(uri: String): Boolean {
        val scheme = runCatching { URI(uri).scheme?.lowercase(Locale.ROOT) }.getOrNull() ?: return false
        return scheme in setOf("http", "https", "file", "rtsp", "rtmp")
    }

    private enum class SeekUnit {
        REL_TIME,
        OTHER;

        companion object {
            fun fromUnit(unit: String): SeekUnit =
                if (unit.uppercase(Locale.ROOT) == "REL_TIME") REL_TIME else OTHER
        }
    }

    companion object {
        private const val TAG = "RendererAvTransport"

        fun formatTime(milliseconds: Long): String {
            val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return "%d:%02d:%02d".format(hours, minutes, seconds)
        }

        /** Accepts "H:mm:ss", "mm:ss" or a plain seconds number. Returns -1 on parse failure. */
        fun parseTime(text: String?): Long {
            val value = text?.trim().orEmpty()
            if (value.isEmpty()) return -1L
            val parts = value.split(':')
            return runCatching {
                when (parts.size) {
                    1 -> value.toDouble().times(1000.0).toLong()
                    2 -> (parts[0].toLong() * 60L + parts[1].toDouble()).times(1000.0).toLong()
                    3 -> (parts[0].toLong() * 3600L + parts[1].toLong() * 60L + parts[2].toDouble())
                        .times(1000.0)
                        .toLong()

                    else -> -1L
                }
            }.getOrDefault(-1L)
        }
    }
}
