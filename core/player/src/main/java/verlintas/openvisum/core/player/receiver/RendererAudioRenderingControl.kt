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

import java.util.Locale
import org.jupnp.model.types.ErrorCode
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.model.types.UnsignedIntegerTwoBytes
import org.jupnp.support.lastchange.LastChange
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl
import org.jupnp.support.renderingcontrol.RenderingControlException
import org.jupnp.support.renderingcontrol.lastchange.ChannelMute
import org.jupnp.support.renderingcontrol.lastchange.ChannelVolume
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlVariable
import org.jupnp.support.model.Channel

class RendererAudioRenderingControl(
    lastChange: LastChange,
    private val host: RendererHost,
    private val onStateChanged: () -> Unit = {},
) : AbstractAudioRenderingControl(lastChange) {

    override fun getCurrentInstanceIds(): Array<UnsignedIntegerFourBytes> =
        arrayOf(AbstractAudioRenderingControl.getDefaultInstanceID())

    override fun getMute(instanceId: UnsignedIntegerFourBytes, channel: String?): Boolean {
        requireMasterChannel(channel)
        return host.isMuted()
    }

    override fun setMute(instanceId: UnsignedIntegerFourBytes, channel: String?, desiredMute: Boolean) {
        requireMasterChannel(channel)
        host.onRemoteSetMute(desiredMute)
        publish()
    }

    override fun getVolume(instanceId: UnsignedIntegerFourBytes, channel: String?): UnsignedIntegerTwoBytes {
        requireMasterChannel(channel)
        return UnsignedIntegerTwoBytes(host.volumePercent().coerceIn(0, 100).toLong())
    }

    override fun setVolume(
        instanceId: UnsignedIntegerFourBytes,
        channel: String?,
        desiredVolume: UnsignedIntegerTwoBytes?,
    ) {
        requireMasterChannel(channel)
        val percent = desiredVolume?.value?.toInt() ?: throw RenderingControlException(ErrorCode.INVALID_ARGS)
        host.onRemoteSetVolume(percent.coerceIn(0, 100))
        publish()
    }

    protected override fun getCurrentChannels(): Array<Channel> = arrayOf(Channel.Master)

    private fun requireMasterChannel(channel: String?) {
        if (channel == null || channel.uppercase(Locale.ROOT) != "MASTER") {
            throw RenderingControlException(ErrorCode.INVALID_ARGS, "Only the Master channel is supported")
        }
    }

    private fun publish() {
        runCatching {
            val lastChange = getLastChange() ?: return
            val instanceId = AbstractAudioRenderingControl.getDefaultInstanceID()
            lastChange.setEventedValue(
                instanceId,
                RenderingControlVariable.Mute(
                    ChannelMute(Channel.Master, host.isMuted()),
                ),
                RenderingControlVariable.Volume(
                    ChannelVolume(Channel.Master, host.volumePercent().coerceIn(0, 100)),
                ),
            )
            onStateChanged()
        }
    }
}
