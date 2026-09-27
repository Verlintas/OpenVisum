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

package verlintas.openvisum.core.data.source

import android.net.Uri
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

data class NetworkEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
)

data class SmbCredentials(
    val domain: String?,
    val username: String?,
    val password: String?,
)

class SmbBrowser {

    suspend fun list(
        host: String,
        port: Int,
        path: String,
        credentials: SmbCredentials,
    ): List<NetworkEntry> = withContext(Dispatchers.IO) {
        val context = buildContext(credentials)
        val directory = SmbFile(buildUrl(host, port, path), context)
        val files = directory.listFiles() ?: emptyArray()
        files.map { file ->
            NetworkEntry(
                name = file.name.trimEnd('/'),
                path = joinPath(path, file.name),
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0L else runCatching { file.length() }.getOrDefault(0L),
                lastModified = runCatching { file.lastModified() }.getOrDefault(0L),
            )
        }.filter { it.name.isNotEmpty() && it.name != "." && it.name != ".." }
            .sortedWith(
                compareByDescending<NetworkEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase() },
            )
    }

    fun playbackUri(host: String, port: Int, path: String): Uri {
        val normalized = path.trimStart('/')
        val portPart = if (port > 0 && port != DEFAULT_PORT) ":$port" else ""
        return Uri.parse("smb://$host$portPart/" + Uri.encode(normalized, "/"))
    }

    fun playbackOptions(credentials: SmbCredentials): List<String> = buildList {
        credentials.username?.takeIf { it.isNotBlank() }?.let { add(":smb-user=$it") }
        credentials.password?.takeIf { it.isNotBlank() }?.let { add(":smb-pwd=$it") }
        credentials.domain?.takeIf { it.isNotBlank() }?.let { add(":smb-domain=$it") }
    }

    private fun buildUrl(host: String, port: Int, path: String): String {
        val portPart = if (port > 0 && port != DEFAULT_PORT) ":$port" else ""
        val normalized = path.trimStart('/')
        return "smb://$host$portPart/$normalized"
    }

    private fun joinPath(base: String, name: String): String {
        val cleanBase = base.trim('/')
        val cleanName = name.trim('/')
        return if (cleanBase.isEmpty()) cleanName else "$cleanBase/$cleanName"
    }

    private fun buildContext(credentials: SmbCredentials): CIFSContext {
        val properties = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.dfs.disabled", "true")
            setProperty("jcifs.smb.client.responseTimeout", "15000")
            setProperty("jcifs.smb.client.connTimeout", "15000")
            setProperty("jcifs.smb.client.soTimeout", "60000")
        }
        val base = BaseContext(PropertyConfiguration(properties))
        val user = credentials.username?.takeIf { it.isNotBlank() }
        return if (user != null) {
            base.withCredentials(
                NtlmPasswordAuthenticator(credentials.domain ?: "", user, credentials.password.orEmpty()),
            )
        } else {
            base.withAnonymousCredentials()
        }
    }

    companion object {
        private const val DEFAULT_PORT = 445
        const val SMB_URI_PREFIX = "smb://"
    }
}
