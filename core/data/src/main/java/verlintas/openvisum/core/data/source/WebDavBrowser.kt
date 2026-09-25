package verlintas.openvisum.core.data.source

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.TimeUnit

class WebDavBrowser(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun list(
        baseUrl: String,
        path: String,
        username: String?,
        password: String?,
    ): List<NetworkEntry> = withContext(Dispatchers.IO) {
        val url = buildDirectoryUrl(baseUrl, path)
        val requestBuilder = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_BODY.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .header("Depth", "1")
        if (!username.isNullOrBlank()) {
            requestBuilder.header("Authorization", Credentials.basic(username, password.orEmpty()))
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("WebDAV error ${response.code} for $url")
            }
            val body = response.body?.byteStream() ?: return@use emptyList()
            parseMultiStatus(body, path)
        }
    }

    fun playbackUrl(baseUrl: String, path: String, username: String?, password: String?): String {
        val url = buildDirectoryUrl(baseUrl, path)
        if (username.isNullOrBlank()) return url
        val httpUrl = url.toHttpUrlOrNull() ?: return url
        return httpUrl.newBuilder()
            .username(username)
            .password(password.orEmpty())
            .build()
            .toString()
    }

    private fun buildDirectoryUrl(baseUrl: String, path: String): String {
        val base = baseUrl.trimEnd('/')
        val cleanPath = path.trim('/')
        return if (cleanPath.isEmpty()) "$base/" else "$base/${cleanPath.split('/').joinToString("/") { encodeSegment(it) }}/"
    }

    private fun encodeSegment(segment: String): String =
        java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    private fun parseMultiStatus(input: java.io.InputStream, currentPath: String): List<NetworkEntry> {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        val entries = mutableListOf<NetworkEntry>()
        var inResponse = false
        var href: String? = null
        var displayName: String? = null
        var contentLength = 0L
        var lastModified = 0L
        var isCollection = false
        var text = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    text = StringBuilder()
                    when (parser.name.lowercase()) {
                        "response" -> {
                            inResponse = true
                            href = null
                            displayName = null
                            contentLength = 0L
                            lastModified = 0L
                            isCollection = false
                        }

                        "collection" -> if (inResponse) isCollection = true
                    }
                }

                XmlPullParser.TEXT -> if (inResponse) text.append(parser.text)

                XmlPullParser.END_TAG -> {
                    if (inResponse) {
                        when (parser.name.lowercase()) {
                            "href" -> href = text.toString().trim()
                            "displayname" -> displayName = text.toString().trim()
                            "getcontentlength" -> contentLength = text.toString().trim().toLongOrNull() ?: 0L
                            "getlastmodified" -> lastModified = parseHttpDate(text.toString().trim())
                            "response" -> {
                                inResponse = false
                                href?.let { rawHref ->
                                    val name = displayName?.takeIf { it.isNotBlank() }
                                        ?: decodeSegment(rawHref.trimEnd('/').substringAfterLast('/'))
                                    if (!isSelf(rawHref, currentPath) && name.isNotBlank()) {
                                        entries += NetworkEntry(
                                            name = name,
                                            path = hrefToPath(rawHref),
                                            isDirectory = isCollection,
                                            sizeBytes = contentLength,
                                            lastModified = lastModified,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return entries.sortedWith(
            compareByDescending<NetworkEntry> { it.isDirectory }
                .thenBy { it.name.lowercase() },
        )
    }

    private fun isSelf(href: String, currentPath: String): Boolean {
        val decoded = decodeSegment(href).trimEnd('/')
        val current = currentPath.trim('/')
        return decoded == current || decoded.endsWith("/$current")
    }

    private fun hrefToPath(href: String): String {
        val marker = href.indexOf("://")
        if (marker < 0) return href.trim('/')
        val afterScheme = href.substring(marker + 3)
        val slash = afterScheme.indexOf('/')
        if (slash < 0) return ""
        return decodeSegment(afterScheme.substring(slash + 1)).trim('/')
    }

    private fun decodeSegment(value: String): String =
        runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun parseHttpDate(value: String): Long =
        runCatching {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
            format.parse(value)?.time ?: 0L
        }.getOrDefault(0L)

    companion object {
        private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8" ?>
<d:propfind xmlns:d="DAV:">
  <d:prop>
    <d:displayname/>
    <d:getcontentlength/>
    <d:getlastmodified/>
    <d:resourcetype/>
  </d:prop>
</d:propfind>"""
    }
}
