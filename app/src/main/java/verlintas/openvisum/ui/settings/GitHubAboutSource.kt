package verlintas.openvisum.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

data class GitHubUser(
    val login: String,
    val name: String?,
    val bio: String?,
    val avatarUrl: String?,
    val publicRepos: Int,
    val followers: Int,
)

data class GitHubRepo(
    val fullName: String,
    val description: String?,
    val stars: Int,
    val forks: Int,
    val openIssues: Int,
    val language: String?,
)

data class GitHubRelease(
    val tagName: String?,
    val name: String?,
    val publishedAt: String?,
)

data class AboutRemoteInfo(
    val user: GitHubUser?,
    val repo: GitHubRepo?,
    val release: GitHubRelease?,
)

object GitHubAboutSource {

    private const val USER_URL = "https://api.github.com/users/Verlintas"
    private const val REPO_URL = "https://api.github.com/repos/Verlintas/OpenVisum"
    private const val RELEASE_URL = "$REPO_URL/releases/latest"

    suspend fun fetch(): AboutRemoteInfo = withContext(Dispatchers.IO) {
        coroutineScope {
            val userDeferred = async { runCatching { fetchUser() }.getOrNull() }
            val repoDeferred = async { runCatching { fetchRepo() }.getOrNull() }
            val releaseDeferred = async { runCatching { fetchRelease() }.getOrNull() }
            AboutRemoteInfo(
                user = userDeferred.await(),
                repo = repoDeferred.await(),
                release = releaseDeferred.await(),
            )
        }
    }

    private fun fetchJson(url: String): kotlinx.serialization.json.JsonObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "OpenVisum")
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (connection.responseCode != 200) {
                error("HTTP ${connection.responseCode} for $url")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return Json.parseToJsonElement(body).jsonObject
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchUser(): GitHubUser {
        val json = fetchJson(USER_URL)
        return GitHubUser(
            login = json["login"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            name = json["name"]?.jsonPrimitive?.contentOrNull,
            bio = json["bio"]?.jsonPrimitive?.contentOrNull,
            avatarUrl = json["avatar_url"]?.jsonPrimitive?.contentOrNull,
            publicRepos = json["public_repos"]?.jsonPrimitive?.intOrNull ?: 0,
            followers = json["followers"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    private fun fetchRepo(): GitHubRepo {
        val json = fetchJson(REPO_URL)
        return GitHubRepo(
            fullName = json["full_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            description = json["description"]?.jsonPrimitive?.contentOrNull,
            stars = json["stargazers_count"]?.jsonPrimitive?.intOrNull ?: 0,
            forks = json["forks_count"]?.jsonPrimitive?.intOrNull ?: 0,
            openIssues = json["open_issues_count"]?.jsonPrimitive?.intOrNull ?: 0,
            language = json["language"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun fetchRelease(): GitHubRelease {
        val json = fetchJson(RELEASE_URL)
        return GitHubRelease(
            tagName = json["tag_name"]?.jsonPrimitive?.contentOrNull,
            name = json["name"]?.jsonPrimitive?.contentOrNull,
            publishedAt = json["published_at"]?.jsonPrimitive?.contentOrNull,
        )
    }
}
