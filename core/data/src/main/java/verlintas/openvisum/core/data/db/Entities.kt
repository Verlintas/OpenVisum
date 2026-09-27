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

package verlintas.openvisum.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["folderKey"]),
        Index(value = ["lastPlayedAt"]),
    ],
)
data class MediaEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateAddedSeconds: Long,
    val folderKey: String?,
    val folderName: String?,
    val source: String,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
)

@Entity(tableName = "saf_folders")
data class SafFolderEntity(
    @PrimaryKey val treeUri: String,
    val name: String,
    val addedAt: Long,
)

@Entity(tableName = "network_sources")
data class NetworkSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String?,
    val passwordEncrypted: String?,
    val domain: String?,
    val basePath: String?,
    val useHttps: Boolean,
    val addedAt: Long,
)

@Entity(tableName = "stream_history")
data class StreamHistoryEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val lastPlayedAt: Long,
)
