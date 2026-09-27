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

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import verlintas.openvisum.core.data.prefs.AppSettings
import verlintas.openvisum.core.data.prefs.PreferencesRepository
import java.io.File
import java.util.concurrent.TimeUnit

class OpenSubtitlesProvider(
    private val context: Context,
    private val preferences: PreferencesRepository,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) : SubtitleProvider {

    override val id: String = PROVIDER_ID

    override val displayName: String = "OpenSubtitles"

    override suspend fun isConfigured(): Boolean =
        !preferences.settings.first().openSubtitlesApiKey.isNullOrBlank()

    override suspend fun search(query: String, language: String): List<SubtitleSearchResult> {
        val settings = preferences.settings.first()
        val apiKey = settings.openSubtitlesApiKey?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val url = BASE_URL.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegment("subtitles")
            ?.addQueryParameter("query", query)
            ?.addQueryParameter("languages", language)
            ?.addQueryParameter("order_by", "download_count")
            ?.addQueryParameter("order", "desc")
            ?.build()
            ?: return emptyList()
        val request = Request.Builder()
            .url(url)
            .header("Api-Key", apiKey)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("OpenSubtitles search failed: HTTP ${response.code}")
                }
                parseSearch(response.body?.string().orEmpty())
            }
        }
    }

    override suspend fun download(result: SubtitleSearchResult): SubtitleDownload {
        val settings = preferences.settings.first()
        val apiKey = settings.openSubtitlesApiKey?.takeIf { it.isNotBlank() }
            ?: error("OpenSubtitles API key is missing")
        val token = login(settings, apiKey)
        val fileId = result.id.toIntOrNull() ?: error("Invalid subtitle id")
        val requestBody = buildJsonObject { put("file_id", fileId) }
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/download")
            .post(requestBody)
            .header("Api-Key", apiKey)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()

        return withContext(Dispatchers.IO) {
            val downloadInfo = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("OpenSubtitles download failed: HTTP ${response.code}")
                }
                val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                val link = json["link"]?.jsonPrimitive?.contentOrNull
                    ?: error("OpenSubtitles did not return a download link")
                val fileName = json["file_name"]?.jsonPrimitive?.contentOrNull
                link to fileName
            }

            val fileRequest = Request.Builder()
                .url(downloadInfo.first)
                .header("User-Agent", USER_AGENT)
                .build()
            val bytes = client.newCall(fileRequest).execute().use { response ->
                if (!response.isSuccessful) error("Subtitle file download failed: HTTP ${response.code}")
                response.body?.bytes() ?: error("Empty subtitle file")
            }
            val rawName = downloadInfo.second
                ?: result.fileName
                ?: "opensubtitles_${result.id}.srt"
            val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val directory = File(context.filesDir, "subtitles").apply { mkdirs() }
            val target = File(directory, safeName)
            target.writeBytes(bytes)
            SubtitleDownload(fileName = safeName, file = target)
        }
    }

    private suspend fun login(settings: AppSettings, apiKey: String): String? {
        val username = settings.openSubtitlesUsername?.takeIf { it.isNotBlank() } ?: return null
        val password = settings.openSubtitlesPassword?.takeIf { it.isNotBlank() } ?: return null
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/login")
            .post(body)
            .header("Api-Key", apiKey)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("OpenSubtitles login failed: HTTP ${response.code}")
                val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                json["token"]?.jsonPrimitive?.contentOrNull
            }
        }
    }

    private fun parseSearch(json: String): List<SubtitleSearchResult> {
        val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull()
            ?: return emptyList()
        val data = root["data"]?.jsonArray ?: return emptyList()
        val results = mutableListOf<SubtitleSearchResult>()
        data.forEach { element ->
            val attributes = element.jsonObject["attributes"]?.jsonObject ?: return@forEach
            val language = attributes["language"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val downloads = attributes["download_count"]?.jsonPrimitive?.intOrNull
            val format = attributes["format"]?.jsonPrimitive?.contentOrNull
            val file = attributes["files"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@forEach
            val fileId = file["file_id"]?.jsonPrimitive?.intOrNull ?: return@forEach
            val fileName = file["file_name"]?.jsonPrimitive?.contentOrNull
            val feature = attributes["feature_details"]?.jsonObject
            val title = feature?.get("title")?.jsonPrimitive?.contentOrNull
                ?: fileName
                ?: "Subtitle #$fileId"
            results += SubtitleSearchResult(
                providerId = id,
                id = fileId.toString(),
                title = title,
                language = language,
                format = format,
                downloads = downloads,
                fileName = fileName,
            )
        }
        return results
    }

    companion object {
        const val PROVIDER_ID = "opensubtitles"
        private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
        private const val USER_AGENT = "OpenVisum v1.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
