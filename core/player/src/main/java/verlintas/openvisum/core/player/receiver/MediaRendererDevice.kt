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

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.model.DefaultServiceManager
import org.jupnp.model.meta.DeviceDetails
import org.jupnp.model.meta.DeviceIdentity
import org.jupnp.model.meta.LocalDevice
import org.jupnp.model.meta.LocalService
import org.jupnp.model.meta.ManufacturerDetails
import org.jupnp.model.meta.ModelDetails
import org.jupnp.model.types.DLNACaps
import org.jupnp.model.types.DLNADoc
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDN
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser
import org.jupnp.support.lastchange.LastChange
import org.jupnp.support.lastchange.LastChangeAwareServiceManager
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser

/**
 * UPnP MediaRenderer device exposing AVTransport, RenderingControl and
 * ConnectionManager, backed by [RendererHost].
 */
class MediaRendererDevice(
    host: RendererHost,
    udn: UDN,
) {

    val device: LocalDevice

    init {
        val binder = AnnotationLocalServiceBinder()

        val avTransportLastChange = LastChange(AVTransportLastChangeParser())
        val renderingControlLastChange = LastChange(RenderingControlLastChangeParser())

        @Suppress("UNCHECKED_CAST")
        val avTransportService = binder.read(RendererAvTransportService::class.java)
            as LocalService<RendererAvTransportService>
        lateinit var avTransportManager: LastChangeAwareServiceManager<RendererAvTransportService>
        avTransportManager = object : LastChangeAwareServiceManager<RendererAvTransportService>(
            avTransportService,
            AVTransportLastChangeParser(),
        ) {
            override fun createServiceInstance(): RendererAvTransportService =
                RendererAvTransportService(avTransportLastChange, host) { avTransportManager.fireLastChange() }
        }
        avTransportService.manager = avTransportManager

        @Suppress("UNCHECKED_CAST")
        val renderingControlService = binder.read(RendererAudioRenderingControl::class.java)
            as LocalService<RendererAudioRenderingControl>
        lateinit var renderingControlManager: LastChangeAwareServiceManager<RendererAudioRenderingControl>
        renderingControlManager = object : LastChangeAwareServiceManager<RendererAudioRenderingControl>(
            renderingControlService,
            RenderingControlLastChangeParser(),
        ) {
            override fun createServiceInstance(): RendererAudioRenderingControl =
                RendererAudioRenderingControl(renderingControlLastChange, host) {
                    renderingControlManager.fireLastChange()
                }
        }
        renderingControlService.manager = renderingControlManager

        @Suppress("UNCHECKED_CAST")
        val connectionManagerService = binder.read(RendererConnectionManagerService::class.java)
            as LocalService<RendererConnectionManagerService>
        val connectionManager = object : DefaultServiceManager<RendererConnectionManagerService>(
            connectionManagerService,
            RendererConnectionManagerService::class.java,
        ) {
            override fun createServiceInstance(): RendererConnectionManagerService =
                RendererConnectionManagerService()
        }
        connectionManagerService.manager = connectionManager

        val details = DeviceDetails(
            host.deviceName(),
            ManufacturerDetails("OpenVisum", "https://github.com/Verlintas/OpenVisum"),
            ModelDetails(
                "OpenVisum",
                "OpenVisum DLNA renderer",
                "1.0",
                "https://github.com/Verlintas/OpenVisum",
            ),
            arrayOf(DLNADoc("DMR", DLNADoc.Version.V1_5)),
            DLNACaps(arrayOf("av-upload", "image-upload", "audio-upload")),
        )

        device = LocalDevice(
            DeviceIdentity(udn),
            UDADeviceType("MediaRenderer", 1),
            details,
            arrayOf<LocalService<*>>(avTransportService, renderingControlService, connectionManagerService),
        )
    }

}
