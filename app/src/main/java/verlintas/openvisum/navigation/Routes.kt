package verlintas.openvisum.navigation

import android.net.Uri

object Routes {

    const val HOME = "home"

    const val LIBRARY = "library"

    const val LIBRARY_PATTERN = "library?tab={tab}&search={search}"

    const val HOME_LEGACY = "home_legacy"

    const val PLAYER_PATTERN = "player?uri={uri}&title={title}&restart={restart}"

    const val BROWSE_FOLDER_PATTERN = "browse?folderKey={folderKey}&name={name}"

    const val SAF_BROWSER_PATTERN = "saf?uri={uri}&name={name}"

    const val SETTINGS = "settings"

    const val SETTINGS_PLAYBACK = "settings/playback"

    const val SETTINGS_SUBTITLES = "settings/subtitles"

    const val SETTINGS_ONLINE = "settings/online"

    const val SETTINGS_APPEARANCE = "settings/appearance"

    const val SETTINGS_ABOUT = "settings/about"

    const val SETTINGS_LICENSES = "settings/licenses"

    const val SETTINGS_CHANGELOG = "settings/changelog"

    const val SETTINGS_FEEDBACK = "settings/feedback"

    const val NETWORK = "network"

    const val NETWORK_BROWSER_PATTERN = "network_browser?sourceId={sourceId}&name={name}"

    fun player(uri: String, title: String? = null, restart: Boolean = false): String {
        val encodedUri = Uri.encode(uri)
        val encodedTitle = Uri.encode(title.orEmpty())
        return "player?uri=$encodedUri&title=$encodedTitle&restart=$restart"
    }

    fun library(tab: Int = 0, search: Boolean = false): String =
        "library?tab=$tab&search=$search"

    fun browseFolder(folderKey: String, name: String): String =
        "browse?folderKey=${Uri.encode(folderKey)}&name=${Uri.encode(name)}"

    fun safBrowser(uri: String, name: String): String =
        "saf?uri=${Uri.encode(uri)}&name=${Uri.encode(name)}"

    fun networkBrowser(sourceId: Long, name: String): String =
        "network_browser?sourceId=$sourceId&name=${Uri.encode(name)}"
}
