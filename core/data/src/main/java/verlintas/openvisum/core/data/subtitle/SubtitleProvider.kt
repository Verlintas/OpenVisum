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
