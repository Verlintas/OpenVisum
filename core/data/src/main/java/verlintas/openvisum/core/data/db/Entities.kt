package verlintas.openvisum.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["folderKey"]),
        Index(value = ["lastPlayedAt"]),
    ],
)
data class MediaEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateAddedSeconds: Long,
    val folderKey: String?,
    val folderName: String?,
    val source: String,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
)

@Entity(tableName = "saf_folders")
data class SafFolderEntity(
    @PrimaryKey val treeUri: String,
    val name: String,
    val addedAt: Long,
)

@Entity(tableName = "network_sources")
data class NetworkSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String?,
    val passwordEncrypted: String?,
    val domain: String?,
    val basePath: String?,
    val useHttps: Boolean,
    val addedAt: Long,
)

@Entity(tableName = "stream_history")
data class StreamHistoryEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val lastPlayedAt: Long,
)
