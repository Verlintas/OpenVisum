package verlintas.openvisum.core.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import verlintas.openvisum.core.data.db.MediaEntity
import verlintas.openvisum.core.data.db.OpenVisumDatabase
import verlintas.openvisum.core.data.model.MediaItem
import verlintas.openvisum.core.data.model.MediaSource
import verlintas.openvisum.core.data.model.toModel
import verlintas.openvisum.core.data.source.MediaStoreScanner
import verlintas.openvisum.core.data.source.SafFolderRepository

data class FolderSummary(
    val folderKey: String,
    val folderName: String,
    val itemCount: Int,
    val source: MediaSource,
)

class MediaRepository(
    private val context: Context,
    database: OpenVisumDatabase,
    private val scanner: MediaStoreScanner,
    val safFolders: SafFolderRepository,
) {

    private val mediaDao = database.mediaDao()

    val allItems: Flow<List<MediaItem>> =
        mediaDao.observeAll().map { entities -> entities.map(MediaEntity::toModel) }

    val favorites: Flow<List<MediaItem>> =
        mediaDao.observeFavorites().map { entities -> entities.map(MediaEntity::toModel) }

    val continueWatching: Flow<List<MediaItem>> =
        mediaDao.observeContinueWatching().map { entities -> entities.map(MediaEntity::toModel) }

    val recent: Flow<List<MediaItem>> =
        mediaDao.observeRecent().map { entities -> entities.map(MediaEntity::toModel) }

    val folders: Flow<List<FolderSummary>> = mediaDao.observeAll().map { entities ->
        entities
            .filter { it.folderKey != null }
            .groupBy { it.folderKey!! to it.source }
            .map { (keyAndSource, items) ->
                FolderSummary(
                    folderKey = keyAndSource.first,
                    folderName = items.firstOrNull()?.folderName ?: keyAndSource.first,
                    itemCount = items.size,
                    source = runCatching { MediaSource.valueOf(keyAndSource.second) }
                        .getOrDefault(MediaSource.MEDIA_STORE),
                )
            }
            .sortedBy { it.folderName.lowercase() }
    }

    fun search(query: String): Flow<List<MediaItem>> =
        mediaDao.search(query).map { entities -> entities.map(MediaEntity::toModel) }

    fun itemsInFolder(folderKey: String): Flow<List<MediaItem>> =
        mediaDao.observeByFolder(folderKey).map { entities -> entities.map(MediaEntity::toModel) }

    suspend fun find(uri: String): MediaItem? = mediaDao.findByUri(uri)?.toModel()

    suspend fun refresh() {
        val scanned = scanner.scanVideos()
        mediaDao.upsertPreservingUserData(scanned)
        val present = scanned.mapTo(mutableSetOf()) { it.uri }
        val stale = mediaDao.urisBySource(MediaSource.MEDIA_STORE.name)
            .filterNot { it in present }
        stale.chunked(400).forEach { chunk -> mediaDao.deleteByUris(chunk) }
    }

    suspend fun toggleFavorite(uri: String, favorite: Boolean) {
        mediaDao.setFavorite(uri, favorite)
    }

    suspend fun saveProgress(uri: String, positionMs: Long, durationMs: Long, timestamp: Long = System.currentTimeMillis()) {
        mediaDao.updateProgress(uri, positionMs, durationMs, timestamp)
    }

    suspend fun removeItem(uri: String) {
        mediaDao.deleteByUri(uri)
    }

    suspend fun addSafFolder(uri: Uri): Result<String> =
        safFolders.addFolder(uri).map { it.name }

    suspend fun removeSafFolder(treeUri: String) = safFolders.removeFolder(treeUri)

    fun safFolderList() = safFolders.observeFolders()
}
