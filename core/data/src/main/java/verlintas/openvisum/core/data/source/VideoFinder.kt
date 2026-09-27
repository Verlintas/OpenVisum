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

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import verlintas.openvisum.core.common.util.NaturalOrder
import java.io.File
import java.util.Locale

data class SiblingVideo(
    val uri: Uri,
    val name: String,
)

/**
 * Finds the other videos in the same folder as the given media, sorted
 * naturally by name ("Ep 2" before "Ep 10").
 */
class VideoFinder(private val context: Context) {

    suspend fun findFor(mediaUri: String): List<SiblingVideo> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(mediaUri)
        val candidates = when {
            uri.scheme == "file" -> findInFileSystem(uri)
            isDocumentUri(uri) -> findViaDocumentFile(uri)
            uri.scheme == "content" -> findViaMediaStore(uri)
            else -> emptyList()
        }
        candidates.sortedWith(compareBy(NaturalOrder) { it.name })
    }

    private fun isDocumentUri(uri: Uri): Boolean =
        uri.scheme == "content" && uri.authority?.contains("documents") == true

    private fun findInFileSystem(uri: Uri): List<SiblingVideo> {
        val path = uri.path ?: return emptyList()
        val parent = File(path).parentFile ?: return emptyList()
        val files = runCatching { parent.listFiles() }.getOrNull() ?: return emptyList()
        return files
            .filter { it.isFile && isVideoFile(it.name) }
            .map { SiblingVideo(Uri.fromFile(it), it.name) }
    }

    private fun findViaDocumentFile(uri: Uri): List<SiblingVideo> {
        val document = DocumentFile.fromSingleUri(context, uri) ?: return emptyList()
        val parent = document.parentFile ?: return emptyList()
        return runCatching {
            parent.listFiles()
                .filter { it.isFile && isVideoFile(it.name.orEmpty()) }
                .mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    SiblingVideo(file.uri, name)
                }
        }.getOrDefault(emptyList())
    }

    private fun findViaMediaStore(uri: Uri): List<SiblingVideo> {
        val videoId = runCatching { ContentUris.parseId(uri) }.getOrNull() ?: return emptyList()
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val relativePath = runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.Video.Media.RELATIVE_PATH),
                "${MediaStore.Video.Media._ID} = ?",
                arrayOf(videoId.toString()),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: return emptyList()

        return runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                ),
                "${MediaStore.Video.Media.RELATIVE_PATH} = ?",
                arrayOf(relativePath),
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: continue
                        add(SiblingVideo(ContentUris.withAppendedId(collection, id), name))
                    }
                }
            }
        }.getOrNull().orEmpty()
    }

    private fun isVideoFile(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase(Locale.ROOT) in VIDEO_EXTENSIONS

    private companion object {
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "m2ts", "flv", "wmv",
            "mpg", "mpeg", "3gp", "rmvb", "rm", "vob", "ogv", "divx",
        )
    }
}
