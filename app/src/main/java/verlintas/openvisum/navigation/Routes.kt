package verlintas.openvisum.navigation

import android.net.Uri

object Routes {

    const val LIBRARY = "library"

    const val HOME = LIBRARY

    const val PLAYER_PATTERN = "player?uri={uri}&title={title}"

    const val BROWSE_FOLDER_PATTERN = "browse?folderKey={folderKey}&name={name}"

    const val SAF_BROWSER_PATTERN = "saf?uri={uri}&name={name}"

    const val SETTINGS = "settings"

    const val SETTINGS_PLAYBACK = "settings/playback"

    const val SETTINGS_SUBTITLES = "settings/subtitles"

    const val SETTINGS_ONLINE = "settings/online"

    const val SETTINGS_APPEARANCE = "settings/appearance"

    const val SETTINGS_ABOUT = "settings/about"

    const val NETWORK = "network"

    const val NETWORK_BROWSER_PATTERN = "network_browser?sourceId={sourceId}&name={name}"

    fun player(uri: String, title: String? = null): String {
        val encodedUri = Uri.encode(uri)
        val encodedTitle = Uri.encode(title.orEmpty())
        return "player?uri=$encodedUri&title=$encodedTitle"
    }

    fun browseFolder(folderKey: String, name: String): String =
        "browse?folderKey=${Uri.encode(folderKey)}&name=${Uri.encode(name)}"

    fun safBrowser(uri: String, name: String): String =
        "saf?uri=${Uri.encode(uri)}&name=${Uri.encode(name)}"

    fun networkBrowser(sourceId: Long, name: String): String =
        "network_browser?sourceId=$sourceId&name=${Uri.encode(name)}"
}
