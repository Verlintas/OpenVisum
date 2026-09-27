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

package verlintas.openvisum.core.data.model

import verlintas.openvisum.core.data.db.MediaEntity

enum class MediaSource { MEDIA_STORE, SAF }

data class MediaItem(
    val uri: String,
    val displayName: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateAddedSeconds: Long,
    val folderKey: String?,
    val folderName: String?,
    val source: MediaSource,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean,
    val lastPlayedAt: Long,
    val playbackPositionMs: Long,
    val playbackDurationMs: Long,
) {
    val resumePositionMs: Long
        get() {
            if (playbackPositionMs <= 5_000L) return 0L
            if (playbackDurationMs <= 0L) return playbackPositionMs
            return if (playbackDurationMs - playbackPositionMs < 15_000L) 0L else playbackPositionMs
        }
}

fun MediaEntity.toModel(): MediaItem = MediaItem(
    uri = uri,
    displayName = displayName,
    title = title,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    dateAddedSeconds = dateAddedSeconds,
    folderKey = folderKey,
    folderName = folderName,
    source = runCatching { MediaSource.valueOf(source) }.getOrDefault(MediaSource.MEDIA_STORE),
    width = width,
    height = height,
    isFavorite = isFavorite,
    lastPlayedAt = lastPlayedAt,
    playbackPositionMs = playbackPositionMs,
    playbackDurationMs = playbackDurationMs,
)

data class SafEntry(
    val uri: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val isPlayable: Boolean,
    val isSubtitle: Boolean,
)
