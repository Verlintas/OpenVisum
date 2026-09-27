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

import org.jupnp.support.connectionmanager.ConnectionManagerService
import org.jupnp.support.model.Protocol
import org.jupnp.support.model.ProtocolInfo
import org.jupnp.support.model.ProtocolInfos

class RendererConnectionManagerService : ConnectionManagerService(
    ProtocolInfos(),
    ProtocolInfos(*sinkProtocols().toTypedArray()),
) {
    private companion object {
        fun sinkProtocols(): List<ProtocolInfo> {
            val mimeTypes = listOf(
                "video/*",
                "audio/*",
                "application/ogg",
                "application/x-mpegurl",
            )
            return mimeTypes.map { mime ->
                ProtocolInfo(Protocol.HTTP_GET, "*", mime, "*")
            }
        }
    }
}
