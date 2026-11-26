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

package verlintas.openvisum.core.player.upnp

import android.content.Context
import android.util.Log
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceImpl
import org.jupnp.android.AndroidRouter
import org.jupnp.android.AndroidUpnpServiceConfiguration
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.registry.Registry
import org.jupnp.transport.Router

/**
 * Shared jUPnP stack used by both the DLNA renderer (receiver) and the DLNA
 * control point (sender). jUPnP's Jetty container is a singleton, so a single
 * UpnpService must be reused by every feature in the process.
 */
object UpnpStack {

    private const val TAG = "UpnpStack"

    private var service: UpnpService? = null
    private var users = 0

    @Synchronized
    fun acquire(context: Context): UpnpService {
        val existing = service
        if (existing != null) {
            users++
            return existing
        }
        val appContext = context.applicationContext
        val created = object : UpnpServiceImpl(AndroidUpnpServiceConfiguration()) {
            override fun createRouter(protocolFactory: ProtocolFactory, registry: Registry): Router =
                AndroidRouter(configuration, protocolFactory, appContext)
        }
        created.startup()
        service = created
        users = 1
        Log.i(TAG, "UPnP stack started")
        return created
    }

    @Synchronized
    fun release() {
        users = maxOf(0, users - 1)
        if (users > 0) return
        val existing = service ?: return
        service = null
        runCatching {
            existing.shutdown()
            Log.i(TAG, "UPnP stack stopped")
        }.onFailure { Log.w(TAG, "Failed to stop the UPnP stack", it) }
    }
}
