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
import java.util.Locale
import org.jupnp.UpnpService
import org.jupnp.controlpoint.ActionCallback
import org.jupnp.model.action.ActionInvocation
import org.jupnp.model.meta.Action
import org.jupnp.model.message.UpnpResponse
import org.jupnp.model.meta.Device as UpnpDevice
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.model.meta.Service
import org.jupnp.model.types.ServiceType
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.model.types.UnsignedIntegerTwoBytes
import org.jupnp.registry.DefaultRegistryListener
import org.jupnp.registry.Registry

/**
 * DLNA control point: discovers UPnP MediaRenderer devices and drives them over
 * AVTransport / RenderingControl SOAP actions.
 *
 * libVLC's Android build does not ship the UPnP renderer discoverer (only the
 * Chromecast/microdns one), so DLNA casting is implemented here on top of
 * jUPnP, which is also used by the receiver.
 */
class UpnpCaster(private val context: Context) {

    data class Device(
        val udn: String,
        val name: String,
        val avTransport: Service<*, *>,
        val renderingControl: Service<*, *>?,
    )

    data class Status(
        val positionMs: Long,
        val durationMs: Long,
        val state: String,
    ) {
        val isPlaying: Boolean get() = state.equals("PLAYING", true)
        val isStopped: Boolean get() = state.equals("STOPPED", true) || state.equals("NO_MEDIA_PRESENT", true)
    }

    var activeDevice: Device? = null

    private var upnpService: UpnpService? = null
    private var listener: DefaultRegistryListener? = null
    private var onDevicesChanged: ((List<Device>) -> Unit)? = null
    private val devices = LinkedHashMap<String, Device>()

    @Volatile
    private var discovering = false

    fun startDiscovery(onDevices: (List<Device>) -> Unit) {
        if (discovering) return
        onDevicesChanged = onDevices
        val service = ensureService() ?: return
        val registryListener = object : DefaultRegistryListener() {
            override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice?) {
                device?.let(::handleDeviceAdded)
            }

            override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {
                device?.let(::handleDeviceRemoved)
            }
        }
        service.registry.addListener(registryListener)
        listener = registryListener
        service.controlPoint.search()
        discovering = true
        Log.i(TAG, "UPnP discovery started")
    }

    fun refresh() {
        upnpService?.controlPoint?.search()
    }

    fun stopDiscovery() {
        if (!discovering) {
            if (activeDevice == null) releaseServiceIfIdle()
            return
        }
        val service = upnpService
        listener?.let { service?.registry?.removeListener(it) }
        listener = null
        devices.clear()
        discovering = false
        onDevicesChanged?.invoke(emptyList())
        if (activeDevice == null) releaseServiceIfIdle()
    }

    fun shutdown() {
        stopDiscovery()
        activeDevice = null
        releaseServiceIfIdle()
    }

    // MARK: - Discovery

    private fun handleDeviceAdded(remote: UpnpDevice<*, *, *>) {
        val deviceType = runCatching { remote.type?.type }.getOrNull()
        if (!deviceType.equals("MediaRenderer", ignoreCase = true)) {
            return
        }
        val avTransport = runCatching {
            remote.findService(ServiceType("schemas-upnp-org", "AVTransport", 1))
        }.getOrNull() ?: return
        val renderingControl = runCatching {
            remote.findService(ServiceType("schemas-upnp-org", "RenderingControl", 1))
        }.getOrNull()
        val udn = runCatching { remote.identity.udn.identifierString }.getOrNull() ?: return
        val name = runCatching { remote.details.friendlyName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: remote.displayString
        val device = Device(udn, name, avTransport, renderingControl)
        synchronized(devices) { devices[udn] = device }
        Log.i(TAG, "UPnP renderer found: $name ($udn)")
        publishDevices()
    }

    private fun handleDeviceRemoved(remote: RemoteDevice) {
        val udn = runCatching { remote.identity.udn.identifierString }.getOrNull() ?: return
        val removed = synchronized(devices) { devices.remove(udn) }
        if (removed != null) {
            Log.i(TAG, "UPnP renderer left: ${removed.name}")
            publishDevices()
        }
    }

    private fun publishDevices() {
        val snapshot = synchronized(devices) { devices.values.toList() }
        onDevicesChanged?.invoke(snapshot)
    }

    // MARK: - Actions

    fun setAVTransportURI(
        device: Device,
        uri: String,
        metadata: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        invoke(
            service = device.avTransport,
            actionName = "SetAVTransportURI",
            inputs = mapOf(
                "InstanceID" to instanceId(),
                "CurrentURI" to uri,
                "CurrentURIMetaData" to metadata,
            ),
            onSuccess = { onResult(true, null) },
            onFailure = { onResult(false, it) },
        )
    }

    fun play(device: Device) {
        invoke(
            service = device.avTransport,
            actionName = "Play",
            inputs = mapOf("InstanceID" to instanceId(), "Speed" to "1"),
        )
    }

    fun pause(device: Device) {
        invoke(
            service = device.avTransport,
            actionName = "Pause",
            inputs = mapOf("InstanceID" to instanceId()),
        )
    }

    fun stop(device: Device) {
        invoke(
            service = device.avTransport,
            actionName = "Stop",
            inputs = mapOf("InstanceID" to instanceId()),
        )
    }

    fun seek(device: Device, positionMs: Long) {
        invoke(
            service = device.avTransport,
            actionName = "Seek",
            inputs = mapOf(
                "InstanceID" to instanceId(),
                "Unit" to "REL_TIME",
                "Target" to formatTime(positionMs),
            ),
        )
    }

    fun setVolume(device: Device, percent: Int) {
        val service = device.renderingControl ?: return
        invoke(
            service = service,
            actionName = "SetVolume",
            inputs = mapOf(
                "InstanceID" to instanceId(),
                "Channel" to "Master",
                "DesiredVolume" to UnsignedIntegerTwoBytes(percent.coerceIn(0, 100).toLong()),
            ),
        )
    }

    fun queryStatus(device: Device, onResult: (Status) -> Unit) {
        invoke(
            service = device.avTransport,
            actionName = "GetPositionInfo",
            inputs = mapOf("InstanceID" to instanceId()),
            onSuccess = { invocation ->
                val position = parseTime(invocation.getOutput("RelTime")?.value as? String)
                val duration = parseTime(invocation.getOutput("TrackDuration")?.value as? String)
                invoke(
                    service = device.avTransport,
                    actionName = "GetTransportInfo",
                    inputs = mapOf("InstanceID" to instanceId()),
                    onSuccess = { info ->
                        val state = info.getOutput("CurrentTransportState")?.value as? String
                            ?: "STOPPED"
                        onResult(Status(position, duration, state))
                    },
                    onFailure = {
                        onResult(Status(position, duration, "STOPPED"))
                    },
                )
            },
            onFailure = {
                onResult(Status(0L, 0L, "STOPPED"))
            },
        )
    }

    // MARK: - Plumbing

    private fun ensureService(): UpnpService? {
        upnpService?.let { return it }
        return runCatching {
            UpnpStack.acquire(context).also { upnpService = it }
        }.getOrElse {
            Log.w(TAG, "Failed to acquire the UPnP stack", it)
            null
        }
    }

    private fun releaseServiceIfIdle() {
        if (discovering || activeDevice != null) return
        val service = upnpService ?: return
        upnpService = null
        UpnpStack.release()
    }

    private fun instanceId(): UnsignedIntegerFourBytes = UnsignedIntegerFourBytes(0)

    private fun invoke(
        service: Service<*, *>?,
        actionName: String,
        inputs: Map<String, Any?>,
        onSuccess: (ActionInvocation<*>) -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        val upnp = upnpService ?: run {
            onFailure("UPnP service unavailable")
            return
        }
        if (service == null) {
            onFailure("No UPnP service for $actionName")
            return
        }
        val action = runCatching { service.getAction(actionName) }.getOrNull() ?: run {
            onFailure("Unsupported action $actionName")
            return
        }
        @Suppress("UNCHECKED_CAST")
        val typedAction = action as Action<Service<*, *>>
        val invocation = ActionInvocation(typedAction)
        inputs.forEach { (key, value) ->
            if (value != null) {
                runCatching { invocation.setInput(key, value) }
            }
        }
        val callback = object : ActionCallback(invocation, upnp.controlPoint) {
            override fun success(invocation: ActionInvocation<*>?) {
                if (invocation != null) {
                    onSuccess(invocation)
                }
            }

            override fun failure(
                invocation: ActionInvocation<*>?,
                response: UpnpResponse?,
                message: String?,
            ) {
                Log.w(TAG, "UPnP $actionName failed: $message")
                onFailure(message ?: "Action failed")
            }
        }
        runCatching { upnp.controlPoint.execute(callback) }
            .onFailure { onFailure(it.message ?: "Action failed") }
    }

    companion object {
        private const val TAG = "UpnpCaster"

        fun buildDidl(title: String, url: String, mimeType: String? = null): String {
            val protocolInfo = "http-get:*:${mimeType ?: "video/mp4"}:*"
            return "<DIDL-Lite " +
                "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
                "<dc:title>${escapeXml(title)}</dc:title>" +
                "<upnp:class>object.item.videoItem</upnp:class>" +
                "<res protocolInfo=\"$protocolInfo\">${escapeXml(url)}</res>" +
                "</item></DIDL-Lite>"
        }

        private fun escapeXml(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        fun formatTime(milliseconds: Long): String {
            val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        }

        fun parseTime(text: String?): Long {
            val value = text?.trim().orEmpty()
            if (value.isEmpty()) return 0L
            val parts = value.split(':')
            return runCatching {
                when (parts.size) {
                    1 -> (value.toDouble() * 1000).toLong()
                    2 -> (parts[0].toLong() * 60L + parts[1].toDouble()).times(1000).toLong()
                    3 -> (parts[0].toLong() * 3600L + parts[1].toLong() * 60L + parts[2].toDouble())
                        .times(1000)
                        .toLong()

                    else -> 0L
                }
            }.getOrDefault(0L)
        }
    }
}
