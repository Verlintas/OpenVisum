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
