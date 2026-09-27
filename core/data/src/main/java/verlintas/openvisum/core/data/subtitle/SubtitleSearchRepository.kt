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

class SubtitleSearchRepository(
    private val providers: List<SubtitleProvider>,
) {

    fun providerNames(): List<String> = providers.map { it.displayName }

    suspend fun configuredProviders(): List<SubtitleProvider> =
        providers.filter { runCatching { it.isConfigured() }.getOrDefault(false) }

    suspend fun search(query: String, language: String): List<SubtitleSearchResult> {
        val results = mutableListOf<SubtitleSearchResult>()
        configuredProviders().forEach { provider ->
            runCatching { provider.search(query, language) }
                .getOrDefault(emptyList())
                .let(results::addAll)
        }
        return results.sortedByDescending { it.downloads ?: 0 }
    }

    suspend fun download(result: SubtitleSearchResult): Result<File> = runCatching {
        val provider = providers.firstOrNull { it.id == result.providerId }
            ?: error("Unknown subtitle provider: ${result.providerId}")
        provider.download(result).file
    }
}
