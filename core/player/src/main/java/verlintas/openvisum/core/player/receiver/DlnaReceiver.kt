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

import android.content.Context
import android.provider.Settings
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceConfiguration
import org.jupnp.UpnpServiceImpl
import org.jupnp.android.AndroidRouter
import org.jupnp.android.AndroidUpnpServiceConfiguration
import org.jupnp.model.types.UDN
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.registry.Registry
import org.jupnp.transport.Router

/**
 * Hosts a UPnP MediaRenderer so other devices on the LAN can cast to this
 * phone/tablet. Runs in the app process (no background service by design:
 * receiving a cast brings the player to the foreground).
 */
class DlnaReceiver(
    context: Context,
    private val host: RendererHost,
) {

    private val appContext = context.applicationContext

    private var upnpService: UpnpService? = null
    private var renderer: MediaRendererDevice? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    val isRunning: Boolean
        get() = upnpService != null

    @Synchronized
    fun start(): Boolean {
        if (upnpService != null) return true
        return runCatching {
            val service = ReceiverUpnpService(AndroidUpnpServiceConfiguration(), appContext)
            service.startup()
            val device = MediaRendererDevice(host, deviceUdn())
            service.registry.addDevice(device.device)
            upnpService = service
            renderer = device
            _running.value = true
            Log.i(TAG, "DLNA receiver started (UDN ${device.device.identity.udn})")
            true
        }.getOrElse { throwable ->
            Log.w(TAG, "Failed to start DLNA receiver", throwable)
            stop()
            false
        }
    }

    @Synchronized
    fun stop() {
        val service = upnpService
        upnpService = null
        val device = renderer
        renderer = null
        _running.value = false
        if (service == null) return
        runCatching {
            device?.let { service.registry.removeDevice(it.device) }
            service.shutdown()
            Log.i(TAG, "DLNA receiver stopped")
        }.onFailure { Log.w(TAG, "Failed to stop DLNA receiver cleanly", it) }
    }

    private fun deviceUdn(): UDN {
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
        val source = "openvisum-dmr-$androidId"
        return UDN(UUID.nameUUIDFromBytes(source.toByteArray()).toString())
    }

    private class ReceiverUpnpService(
        configuration: UpnpServiceConfiguration,
        private val context: Context,
    ) : UpnpServiceImpl(configuration) {

        override fun createRouter(protocolFactory: ProtocolFactory, registry: Registry): Router =
            AndroidRouter(configuration, protocolFactory, context)
    }

    private companion object {
        const val TAG = "DlnaReceiver"
    }
}
