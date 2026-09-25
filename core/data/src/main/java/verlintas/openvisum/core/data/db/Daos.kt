package verlintas.openvisum.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY dateAddedSeconds DESC")
    fun observeAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<MediaEntity>>

    @Query(
        "SELECT * FROM media_items WHERE playbackPositionMs > 0 AND playbackDurationMs > 0 " +
            "AND playbackDurationMs - playbackPositionMs > 15000 " +
            "ORDER BY lastPlayedAt DESC LIMIT 20",
    )
    fun observeContinueWatching(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 30")
    fun observeRecent(): Flow<List<MediaEntity>>

    @Query(
        "SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' " +
            "OR displayName LIKE '%' || :query || '%' ORDER BY dateAddedSeconds DESC",
    )
    fun search(query: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE folderKey = :folderKey ORDER BY displayName COLLATE NOCASE ASC")
    fun observeByFolder(folderKey: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): MediaEntity?

    @Query("SELECT * FROM media_items WHERE folderKey = :folderKey")
    suspend fun findByFolder(folderKey: String): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<MediaEntity>): List<Long>

    @Query(
        "UPDATE media_items SET displayName = :displayName, title = :title, durationMs = :durationMs, " +
            "sizeBytes = :sizeBytes, mimeType = :mimeType, dateAddedSeconds = :dateAddedSeconds, " +
            "folderKey = :folderKey, folderName = :folderName, source = :source, width = :width, height = :height " +
            "WHERE uri = :uri",
    )
    suspend fun updateMetadata(
        uri: String,
        displayName: String,
        title: String,
        durationMs: Long,
        sizeBytes: Long,
        mimeType: String?,
        dateAddedSeconds: Long,
        folderKey: String?,
        folderName: String?,
        source: String,
        width: Int,
        height: Int,
    )

    @Query("UPDATE media_items SET isFavorite = :favorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, favorite: Boolean)

    @Query(
        "UPDATE media_items SET playbackPositionMs = :positionMs, playbackDurationMs = :durationMs, " +
            "lastPlayedAt = :timestamp WHERE uri = :uri",
    )
    suspend fun updateProgress(uri: String, positionMs: Long, durationMs: Long, timestamp: Long)

    @Query("DELETE FROM media_items WHERE source = :source AND uri NOT IN (:presentUris)")
    suspend fun deleteMissingFromSource(source: String, presentUris: List<String>)

    @Query("SELECT uri FROM media_items WHERE source = :source")
    suspend fun urisBySource(source: String): List<String>

    @Query("DELETE FROM media_items WHERE uri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Transaction
    suspend fun upsertPreservingUserData(items: List<MediaEntity>) {
        if (items.isEmpty()) return
        insertIgnore(items)
        items.forEach { item ->
            updateMetadata(
                uri = item.uri,
                displayName = item.displayName,
                title = item.title,
                durationMs = item.durationMs,
                sizeBytes = item.sizeBytes,
                mimeType = item.mimeType,
                dateAddedSeconds = item.dateAddedSeconds,
                folderKey = item.folderKey,
                folderName = item.folderName,
                source = item.source,
                width = item.width,
                height = item.height,
            )
        }
    }
}

@Dao
interface SafFolderDao {

    @Query("SELECT * FROM saf_folders ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<SafFolderEntity>>

    @Query("SELECT * FROM saf_folders WHERE treeUri = :treeUri LIMIT 1")
    suspend fun find(treeUri: String): SafFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: SafFolderEntity)

    @Query("DELETE FROM saf_folders WHERE treeUri = :treeUri")
    suspend fun delete(treeUri: String)
}

@Dao
interface NetworkSourceDao {

    @Query("SELECT * FROM network_sources ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<NetworkSourceEntity>>

    @Query("SELECT * FROM network_sources WHERE id = :id LIMIT 1")
    suspend fun find(id: Long): NetworkSourceEntity?

    @Query("SELECT * FROM network_sources ORDER BY addedAt ASC")
    suspend fun all(): List<NetworkSourceEntity>

    @Insert
    suspend fun insert(source: NetworkSourceEntity): Long

    @Update
    suspend fun update(source: NetworkSourceEntity)

    @Query("DELETE FROM network_sources WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface StreamHistoryDao {

    @Query("SELECT * FROM stream_history ORDER BY lastPlayedAt DESC LIMIT 30")
    fun observeAll(): Flow<List<StreamHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: StreamHistoryEntity)

    @Query("DELETE FROM stream_history WHERE url = :url")
    suspend fun delete(url: String)
}
