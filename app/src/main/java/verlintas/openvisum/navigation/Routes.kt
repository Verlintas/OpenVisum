package verlintas.openvisum.navigation

import android.net.Uri

object Routes {

    const val HOME = "home"

    const val PLAYER_PATTERN = "player?uri={uri}&title={title}"

    fun player(uri: String, title: String? = null): String {
        val encodedUri = Uri.encode(uri)
        val encodedTitle = Uri.encode(title.orEmpty())
        return "player?uri=$encodedUri&title=$encodedTitle"
    }
}
