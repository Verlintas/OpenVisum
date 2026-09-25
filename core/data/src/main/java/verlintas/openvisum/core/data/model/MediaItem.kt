package verlintas.openvisum.core.data.model

import verlintas.openvisum.core.data.db.MediaEntity

enum class MediaSource { MEDIA_STORE, SAF }

data class MediaItem(
    val uri: String,
    val displayName: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateAddedSeconds: Long,
    val folderKey: String?,
    val folderName: String?,
    val source: MediaSource,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean,
    val lastPlayedAt: Long,
    val playbackPositionMs: Long,
    val playbackDurationMs: Long,
) {
    val resumePositionMs: Long
        get() {
            if (playbackPositionMs <= 5_000L) return 0L
            if (playbackDurationMs <= 0L) return playbackPositionMs
            return if (playbackDurationMs - playbackPositionMs < 15_000L) 0L else playbackPositionMs
        }
}

fun MediaEntity.toModel(): MediaItem = MediaItem(
    uri = uri,
    displayName = displayName,
    title = title,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    dateAddedSeconds = dateAddedSeconds,
    folderKey = folderKey,
    folderName = folderName,
    source = runCatching { MediaSource.valueOf(source) }.getOrDefault(MediaSource.MEDIA_STORE),
    width = width,
    height = height,
    isFavorite = isFavorite,
    lastPlayedAt = lastPlayedAt,
    playbackPositionMs = playbackPositionMs,
    playbackDurationMs = playbackDurationMs,
)

data class SafEntry(
    val uri: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val isPlayable: Boolean,
    val isSubtitle: Boolean,
)
