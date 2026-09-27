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

package verlintas.openvisum.core.data.subtitle

import java.io.File

data class SubtitleSearchResult(
    val providerId: String,
    val id: String,
    val title: String,
    val language: String,
    val format: String?,
    val downloads: Int?,
    val fileName: String?,
)

data class SubtitleDownload(
    val fileName: String,
    val file: File,
)

interface SubtitleProvider {

    val id: String

    val displayName: String

    suspend fun isConfigured(): Boolean

    suspend fun search(query: String, language: String): List<SubtitleSearchResult>

    suspend fun download(result: SubtitleSearchResult): SubtitleDownload
}
