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

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

class MediaStreamServer(private val context: Context) {

    data class Source(
        val uri: Uri,
        val mimeType: String?,
        val sizeBytes: Long,
        val displayName: String?,
    )

    private var serverSocket: ServerSocket? = null
    private var source: Source? = null

    var port: Int = 0
        private set

    val streamUrl: String?
        get() {
            val localPort = port
            val host = localIpv4Address() ?: return null
            if (localPort <= 0) return null
            return "http://$host:$localPort/media"
        }

    fun start(uri: Uri): Boolean {
        stop()
        val metadata = readMetadata(uri) ?: return false
        return runCatching {
            val socket = ServerSocket(0)
            serverSocket = socket
            source = metadata
            port = socket.localPort
            thread(name = "media-stream-server", isDaemon = true) {
                acceptLoop(socket)
            }
            Log.d(TAG, "Stream server started on port $port for $uri")
            true
        }.getOrElse { throwable ->
            Log.w(TAG, "Unable to start stream server", throwable)
            stop()
            false
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        source = null
        port = 0
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (_: SocketException) {
                break
            } catch (io: IOException) {
                Log.w(TAG, "accept failed", io)
                continue
            }
            thread(name = "media-stream-client", isDaemon = true) {
                runCatching { handleClient(client) }
                    .onFailure { if (it !is IOException) Log.w(TAG, "stream failed", it) }
                runCatching { client.close() }
            }
        }
    }

    private fun handleClient(client: Socket) {
        val source = this.source ?: return
        client.soTimeout = 15_000
        val input = client.getInputStream()
        val requestLine = readLine(input) ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return
        val method = parts[0]
        var rangeStart: Long? = null
        var rangeEnd: Long? = null
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Range:", ignoreCase = true)) {
                val value = line.substringAfter("=", "").trim()
                val range = value.substringBefore(",").trim()
                val startText = range.substringBefore("-").trim()
                val endText = range.substringAfter("-", "").trim()
                rangeStart = startText.toLongOrNull()
                rangeEnd = endText.toLongOrNull()
            }
        }
        if (method != "GET" && method != "HEAD") {
            writeStatus(client, "405 Method Not Allowed")
            return
        }

        val total = source.sizeBytes
        val output = client.getOutputStream()
        val contentType = source.mimeType?.takeIf { it.isNotBlank() } ?: "video/*"

        if (total <= 0L || rangeStart == null) {
            writeStatus(
                client,
                "200 OK",
                listOf(
                    "Content-Type" to contentType,
                    "Accept-Ranges" to if (total > 0L) "bytes" else "none",
                    if (total > 0L) "Content-Length" to "$total" else null,
                ),
            )
            if (method == "GET") {
                openStream(source, 0L)?.use { stream ->
                    stream.copyTo(output, bufferSize = STREAM_BUFFER_BYTES)
                }
            }
        } else {
            val start = rangeStart.coerceIn(0L, total - 1)
            val end = (rangeEnd ?: (total - 1)).coerceIn(start, total - 1)
            val length = end - start + 1
            writeStatus(
                client,
                "206 Partial Content",
                listOf(
                    "Content-Type" to contentType,
                    "Accept-Ranges" to "bytes",
                    "Content-Length" to "$length",
                    "Content-Range" to "bytes $start-$end/$total",
                ),
            )
            if (method == "GET") {
                openStream(source, start)?.use { stream ->
                    copyRange(stream, output, length)
                }
            }
        }
        runCatching { output.flush() }
    }

    private fun openStream(source: Source, startOffset: Long): java.io.InputStream? {
        val stream = runCatching {
            context.contentResolver.openInputStream(source.uri)
        }.getOrNull() ?: return null
        if (startOffset > 0L && stream.skip(startOffset) != startOffset) {
            var remaining = startOffset
            while (remaining > 0) {
                val skipped = stream.skip(remaining)
                if (skipped <= 0) break
                remaining -= skipped
            }
        }
        return stream
    }

    private fun copyRange(input: java.io.InputStream, output: OutputStream, length: Long) {
        val buffer = ByteArray(STREAM_BUFFER_BYTES)
        var remaining = length
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read <= 0) break
            runCatching { output.write(buffer, 0, read) }.onFailure { return }
            remaining -= read
        }
    }

    private fun writeStatus(
        client: Socket,
        status: String,
        headers: List<Pair<String, String>?> = emptyList(),
    ) {
        val builder = StringBuilder()
        builder.append("HTTP/1.1 ").append(status).append("\r\n")
        builder.append("Connection: close\r\n")
        headers.filterNotNull().forEach { (name, value) ->
            builder.append(name).append(": ").append(value).append("\r\n")
        }
        builder.append("\r\n")
        client.getOutputStream().write(builder.toString().toByteArray())
    }

    private fun readLine(input: java.io.InputStream): String? {
        val builder = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte < 0) return builder.toString().ifEmpty { null }
            if (byte == '\n'.code) {
                if (builder.isNotEmpty() && builder.last() == '\r') {
                    builder.deleteCharAt(builder.length - 1)
                }
                return builder.toString()
            }
            builder.append(byte.toChar())
        }
    }

    private fun readMetadata(uri: Uri): Source? {
        val resolver = context.contentResolver
        var size = -1L
        var name: String? = null
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE, OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        }
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
            ?: name?.substringAfterLast('.', "")?.let { extension ->
                when (extension.lowercase()) {
                    "mp4", "m4v" -> "video/mp4"
                    "mkv" -> "video/x-matroska"
                    "webm" -> "video/webm"
                    "avi" -> "video/x-msvideo"
                    "mov" -> "video/quicktime"
                    "ts" -> "video/mp2t"
                    "flv" -> "video/x-flv"
                    "wmv" -> "video/x-ms-wmv"
                    else -> null
                }
            }
        if (size <= 0L && mime == null && name == null) return null
        return Source(uri = uri, mimeType = mime, sizeBytes = size, displayName = name)
    }

    private fun localIpv4Address(): String? {
        return runCatching {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
            val network = connectivity.activeNetwork ?: return@runCatching null
            val properties = connectivity.getLinkProperties(network) ?: return@runCatching null
            properties.linkAddresses
                .map { it.address }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }

    private companion object {
        const val TAG = "MediaStreamServer"
        const val STREAM_BUFFER_BYTES = 64 * 1024
    }
}
