package verlintas.openvisum.core.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import verlintas.openvisum.core.data.db.NetworkSourceDao
import verlintas.openvisum.core.data.db.NetworkSourceEntity
import verlintas.openvisum.core.data.db.StreamHistoryDao
import verlintas.openvisum.core.data.db.StreamHistoryEntity
import verlintas.openvisum.core.data.security.SecretCipher
import verlintas.openvisum.core.data.source.NetworkEntry
import verlintas.openvisum.core.data.source.SmbBrowser
import verlintas.openvisum.core.data.source.SmbCredentials
import verlintas.openvisum.core.data.source.WebDavBrowser

enum class NetworkSourceType { SMB, WEBDAV }

data class NetworkSource(
    val id: Long,
    val type: NetworkSourceType,
    val name: String,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val domain: String?,
    val basePath: String?,
    val useHttps: Boolean,
)

data class StreamHistoryItem(
    val url: String,
    val title: String?,
    val lastPlayedAt: Long,
)

data class NetworkPlaybackTarget(
    val uri: Uri,
    val options: List<String>,
    val title: String,
)

class NetworkRepository(
    private val sourceDao: NetworkSourceDao,
    private val streamDao: StreamHistoryDao,
    private val cipher: SecretCipher,
    private val smbBrowser: SmbBrowser,
    private val webDavBrowser: WebDavBrowser,
) {

    fun sources(): Flow<List<NetworkSource>> =
        sourceDao.observeAll().map { list -> list.map { it.toModel() } }

    fun streamHistory(): Flow<List<StreamHistoryItem>> =
        streamDao.observeAll().map { list ->
            list.map { StreamHistoryItem(it.url, it.title, it.lastPlayedAt) }
        }

    suspend fun addSource(
        type: NetworkSourceType,
        name: String,
        host: String,
        port: Int,
        username: String?,
        password: String?,
        domain: String?,
        basePath: String?,
        useHttps: Boolean,
    ): Long = sourceDao.insert(
        NetworkSourceEntity(
            type = type.name,
            name = name,
            host = host,
            port = port,
            username = username?.takeIf { it.isNotBlank() },
            passwordEncrypted = password?.takeIf { it.isNotBlank() }?.let(cipher::encrypt),
            domain = domain?.takeIf { it.isNotBlank() },
            basePath = basePath?.takeIf { it.isNotBlank() },
            useHttps = useHttps,
            addedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun updateSource(source: NetworkSource) {
        sourceDao.update(
            NetworkSourceEntity(
                id = source.id,
                type = source.type.name,
                name = source.name,
                host = source.host,
                port = source.port,
                username = source.username,
                passwordEncrypted = source.password?.let(cipher::encrypt),
                domain = source.domain,
                basePath = source.basePath,
                useHttps = source.useHttps,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteSource(id: Long) = sourceDao.delete(id)

    suspend fun browse(sourceId: Long, path: String): Result<List<NetworkEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = sourceDao.find(sourceId)
                    ?: error("Network source not found")
                when (source.type) {
                    NetworkSourceType.SMB.name -> smbBrowser.list(
                        host = source.host,
                        port = source.port,
                        path = path,
                        credentials = SmbCredentials(source.domain, source.username, decrypt(source.passwordEncrypted)),
                    )

                    else -> webDavBrowser.list(
                        baseUrl = baseUrl(source),
                        path = path,
                        username = source.username,
                        password = decrypt(source.passwordEncrypted),
                    )
                }
            }
        }

    suspend fun playbackTarget(sourceId: Long, entry: NetworkEntry): Result<NetworkPlaybackTarget> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = sourceDao.find(sourceId)
                    ?: error("Network source not found")
                when (source.type) {
                    NetworkSourceType.SMB.name -> NetworkPlaybackTarget(
                        uri = smbBrowser.playbackUri(source.host, source.port, entry.path),
                        options = smbBrowser.playbackOptions(
                            SmbCredentials(source.domain, source.username, decrypt(source.passwordEncrypted)),
                        ),
                        title = entry.name,
                    )

                    else -> NetworkPlaybackTarget(
                        uri = Uri.parse(
                            webDavBrowser.playbackUrl(
                                baseUrl = baseUrl(source),
                                path = entry.path,
                                username = source.username,
                                password = decrypt(source.passwordEncrypted),
                            ),
                        ),
                        options = emptyList(),
                        title = entry.name,
                    )
                }
            }
        }

    suspend fun playbackOptionsForUri(uri: String): List<String> {
        if (!uri.startsWith(SmbBrowser.SMB_URI_PREFIX)) return emptyList()
        return runCatching {
            val parsed = Uri.parse(uri)
            val host = parsed.host ?: return emptyList()
            val port = if (parsed.port > 0) parsed.port else 445
            val source = sourceDao.all().firstOrNull {
                it.type == NetworkSourceType.SMB.name && it.host == host && (it.port == port || it.port <= 0)
            } ?: return emptyList()
            smbBrowser.playbackOptions(
                SmbCredentials(source.domain, source.username, decrypt(source.passwordEncrypted)),
            )
        }.getOrDefault(emptyList())
    }

    suspend fun rememberStream(url: String, title: String?) {
        streamDao.upsert(
            StreamHistoryEntity(
                url = url,
                title = title,
                lastPlayedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun forgetStream(url: String) = streamDao.delete(url)

    private fun baseUrl(source: NetworkSourceEntity): String {
        val scheme = if (source.useHttps) "https" else "http"
        val port = when {
            source.port > 0 -> ":${source.port}"
            else -> ""
        }
        val base = source.basePath?.trim('/').orEmpty()
        val path = if (base.isEmpty()) "" else "/$base"
        return "$scheme://${source.host}$port$path"
    }

    private fun decrypt(encrypted: String?): String? = encrypted?.let(cipher::decrypt)

    private fun NetworkSourceEntity.toModel(): NetworkSource = NetworkSource(
        id = id,
        type = runCatching { NetworkSourceType.valueOf(type) }.getOrDefault(NetworkSourceType.SMB),
        name = name,
        host = host,
        port = port,
        username = username,
        password = decrypt(passwordEncrypted),
        domain = domain,
        basePath = basePath,
        useHttps = useHttps,
    )
}
