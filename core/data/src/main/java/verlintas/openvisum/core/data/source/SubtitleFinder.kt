package verlintas.openvisum.core.data.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import verlintas.openvisum.core.common.util.SubtitleMatcher
import java.io.File

data class SubtitleFile(
    val uri: Uri,
    val name: String,
    val language: String?,
    val score: Int,
)

class SubtitleFinder(private val context: Context) {

    suspend fun findFor(
        mediaUri: String,
        displayName: String?,
    ): List<SubtitleFile> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(mediaUri)
        val candidates = when {
            uri.scheme == "file" -> findInFileSystem(uri)
            isDocumentUri(uri) -> findViaDocumentFile(uri)
            uri.scheme == "content" -> findViaMediaStore(uri)
            else -> emptyList()
        }
        if (candidates.isEmpty()) return@withContext emptyList()

        val videoName = displayName
            ?: runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull()
            ?: uri.lastPathSegment.orEmpty()

        val byName = candidates.groupBy { it.second }
        SubtitleMatcher.findMatches(videoName, candidates.map { it.second })
            .mapNotNull { match ->
                val candidate = byName[match.fileName]?.firstOrNull() ?: return@mapNotNull null
                SubtitleFile(
                    uri = candidate.first,
                    name = match.fileName,
                    language = match.language,
                    score = match.score,
                )
            }
    }

    private fun isDocumentUri(uri: Uri): Boolean =
        uri.scheme == "content" && uri.authority?.contains("documents") == true

    private fun findInFileSystem(uri: Uri): List<Pair<Uri, String>> {
        val path = uri.path ?: return emptyList()
        val parent = File(path).parentFile ?: return emptyList()
        val files = runCatching { parent.listFiles() }.getOrNull() ?: return emptyList()
        return files
            .filter { it.isFile && SubtitleMatcher.isSubtitleFile(it.name) }
            .map { Uri.fromFile(it) to it.name }
    }

    private fun findViaDocumentFile(uri: Uri): List<Pair<Uri, String>> {
        val document = DocumentFile.fromSingleUri(context, uri) ?: return emptyList()
        val parent = document.parentFile ?: return emptyList()
        return runCatching {
            parent.listFiles()
                .filter { it.isFile && SubtitleMatcher.isSubtitleFile(it.name.orEmpty()) }
                .mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    file.uri to name
                }
        }.getOrDefault(emptyList())
    }

    private fun findViaMediaStore(uri: Uri): List<Pair<Uri, String>> {
        val videoId = runCatching { ContentUris.parseId(uri) }.getOrNull() ?: return emptyList()
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val relativePath = runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.Files.FileColumns.RELATIVE_PATH),
                "${MediaStore.Files.FileColumns._ID} = ?",
                arrayOf(videoId.toString()),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: return emptyList()

        val subtitleRows = runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                ),
                "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?",
                arrayOf(relativePath, MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE.toString()),
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: continue
                        add(ContentUris.withAppendedId(collection, id) to name)
                    }
                }
            }
        }.getOrNull() ?: return emptyList()

        return subtitleRows.filter { (subtitleUri, _) -> canRead(subtitleUri) }
    }

    private fun canRead(uri: Uri): Boolean =
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
}
