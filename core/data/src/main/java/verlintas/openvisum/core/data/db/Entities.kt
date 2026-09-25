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
