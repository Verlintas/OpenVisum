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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import verlintas.openvisum.core.common.util.SubtitleMatcher
import verlintas.openvisum.core.data.db.SafFolderDao
import verlintas.openvisum.core.data.db.SafFolderEntity
import verlintas.openvisum.core.data.model.SafEntry
import java.util.Locale

class SafFolderRepository(
    private val context: Context,
    private val dao: SafFolderDao,
) {

    fun observeFolders(): Flow<List<SafFolderEntity>> = dao.observeAll()

    suspend fun addFolder(uri: Uri): Result<SafFolderEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            val document = DocumentFile.fromTreeUri(context, uri)
            val entity = SafFolderEntity(
                treeUri = uri.toString(),
                name = document?.name ?: uri.lastPathSegment.orEmpty(),
                addedAt = System.currentTimeMillis(),
            )
            dao.upsert(entity)
            entity
        }
    }

    suspend fun removeFolder(treeUri: String) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(treeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        dao.delete(treeUri)
    }

    suspend fun listChildren(folderUri: Uri): List<SafEntry> = withContext(Dispatchers.IO) {
        val document = DocumentFile.fromTreeUri(context, folderUri)
            ?: DocumentFile.fromSingleUri(context, folderUri)
            ?: return@withContext emptyList()
        document.listFiles()
            .map { file ->
                val extension = file.name?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
                SafEntry(
                    uri = file.uri.toString(),
                    name = file.name ?: file.uri.lastPathSegment.orEmpty(),
                    isDirectory = file.isDirectory,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    isPlayable = PLAYABLE_EXTENSIONS.contains(extension),
                    isSubtitle = file.isFile && SubtitleMatcher.SUBTITLE_EXTENSIONS.contains(extension),
                )
            }
            .sortedWith(
                compareByDescending<SafEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase(Locale.ROOT) },
            )
    }

    suspend fun findSiblingSubtitles(fileUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val file = DocumentFile.fromSingleUri(context, fileUri) ?: return@withContext emptyList()
        val parent = file.parentFile ?: return@withContext emptyList()
        val fileName = file.name ?: return@withContext emptyList()
        val siblings = parent.listFiles()
            .mapNotNull { it.name }
            .toList()
        SubtitleMatcher.findMatches(fileName, siblings).mapNotNull { match ->
            parent.findFile(match.fileName)?.uri
        }
    }

    companion object {
        val PLAYABLE_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "ts", "m2ts", "mts",
            "mpg", "mpeg", "vob", "rmvb", "rm", "3gp", "ogv", "divx", "m4v", "f4v",
            "mp3", "flac", "aac", "m4a", "ogg", "opus", "wav", "wma", "ape", "alac",
        )
    }
}
