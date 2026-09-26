package verlintas.openvisum.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import verlintas.openvisum.core.data.security.SecretCipher
import java.io.IOException
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class SortOrder { DATE_DESC, NAME_ASC, SIZE_DESC, DURATION_DESC }

data class AppSettings(
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val hardwareDecoding: Boolean = true,
    val disableDirectRendering: Boolean = false,
    val preferredSubtitleLanguages: List<String> = defaultSubtitleLanguages(),
    val preferredAudioLanguages: List<String> = defaultAudioLanguages(),
    val autoLoadExternalSubtitles: Boolean = true,
    val subtitleScale: Float = 1.0f,
    val subtitleBold: Boolean = false,
    val subtitleColor: Int? = null,
    val stereoMode: Int = 0,
    val openSubtitlesApiKey: String? = null,
    val openSubtitlesUsername: String? = null,
    val openSubtitlesPassword: String? = null,
    val themeMode: Int = THEME_SYSTEM,
    val themeColor: String = "brand",
) {
    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }
}

fun defaultSubtitleLanguages(): List<String> = buildList {
    add("zh-Hans")
    add("zh-Hant")
    add("zh")
    add("en")
    Locale.getDefault().language.takeIf { it.isNotBlank() }?.let { add(it) }
}.distinct()

fun defaultAudioLanguages(): List<String> = listOf(Locale.getDefault().language, "en").filter { it.isNotBlank() }

class PreferencesRepository(
    private val context: Context,
    private val cipher: SecretCipher,
) {

    private object Keys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        val DISABLE_DIRECT_RENDERING = booleanPreferencesKey("disable_direct_rendering")
        val PREFERRED_SUBTITLE_LANGUAGES = stringSetPreferencesKey("preferred_subtitle_languages")
        val PREFERRED_AUDIO_LANGUAGES = stringSetPreferencesKey("preferred_audio_languages")
        val AUTO_LOAD_EXTERNAL_SUBTITLES = booleanPreferencesKey("auto_load_external_subtitles")
        val SUBTITLE_SCALE = floatPreferencesKey("subtitle_scale")
        val SUBTITLE_BOLD = booleanPreferencesKey("subtitle_bold")
        val SUBTITLE_COLOR = intPreferencesKey("subtitle_color")
        val STEREO_MODE = intPreferencesKey("stereo_mode")
        val OPEN_SUBTITLES_API_KEY = stringPreferencesKey("opensubtitles_api_key")
        val OPEN_SUBTITLES_USERNAME = stringPreferencesKey("opensubtitles_username")
        val OPEN_SUBTITLES_PASSWORD = stringPreferencesKey("opensubtitles_password")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                sortOrder = preferences[Keys.SORT_ORDER]
                    ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                    ?: SortOrder.DATE_DESC,
                hardwareDecoding = preferences[Keys.HARDWARE_DECODING] ?: true,
                disableDirectRendering = preferences[Keys.DISABLE_DIRECT_RENDERING] ?: false,
                preferredSubtitleLanguages = preferences[Keys.PREFERRED_SUBTITLE_LANGUAGES]?.toList()
                    ?: defaultSubtitleLanguages(),
                preferredAudioLanguages = preferences[Keys.PREFERRED_AUDIO_LANGUAGES]?.toList()
                    ?: defaultAudioLanguages(),
                autoLoadExternalSubtitles = preferences[Keys.AUTO_LOAD_EXTERNAL_SUBTITLES] ?: true,
                subtitleScale = preferences[Keys.SUBTITLE_SCALE] ?: 1.0f,
                subtitleBold = preferences[Keys.SUBTITLE_BOLD] ?: false,
                subtitleColor = preferences[Keys.SUBTITLE_COLOR]?.takeIf { it >= 0 },
                stereoMode = preferences[Keys.STEREO_MODE] ?: 0,
                openSubtitlesApiKey = preferences[Keys.OPEN_SUBTITLES_API_KEY],
                openSubtitlesUsername = preferences[Keys.OPEN_SUBTITLES_USERNAME],
                openSubtitlesPassword = preferences[Keys.OPEN_SUBTITLES_PASSWORD]?.let(cipher::decrypt),
                themeMode = preferences[Keys.THEME_MODE] ?: AppSettings.THEME_SYSTEM,
                themeColor = preferences[Keys.THEME_COLOR] ?: "brand",
            )
        }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }
    }

    suspend fun setHardwareDecoding(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_DECODING] = enabled }
    }

    suspend fun setDisableDirectRendering(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DISABLE_DIRECT_RENDERING] = enabled }
    }

    suspend fun setPreferredSubtitleLanguages(languages: List<String>) {
        context.dataStore.edit { it[Keys.PREFERRED_SUBTITLE_LANGUAGES] = languages.toSet() }
    }

    suspend fun setPreferredAudioLanguages(languages: List<String>) {
        context.dataStore.edit { it[Keys.PREFERRED_AUDIO_LANGUAGES] = languages.toSet() }
    }

    suspend fun setAutoLoadExternalSubtitles(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LOAD_EXTERNAL_SUBTITLES] = enabled }
    }

    suspend fun setSubtitleStyle(scale: Float, bold: Boolean, color: Int?) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SUBTITLE_SCALE] = scale
            preferences[Keys.SUBTITLE_BOLD] = bold
            preferences[Keys.SUBTITLE_COLOR] = color ?: -1
        }
    }

    suspend fun setStereoMode(mode: Int) {
        context.dataStore.edit { it[Keys.STEREO_MODE] = mode }
    }

    suspend fun setOpenSubtitlesCredentials(apiKey: String, username: String?, password: String?) {
        context.dataStore.edit { preferences ->
            preferences[Keys.OPEN_SUBTITLES_API_KEY] = apiKey
            preferences[Keys.OPEN_SUBTITLES_USERNAME] = username.orEmpty()
            preferences[Keys.OPEN_SUBTITLES_PASSWORD] = password
                ?.takeIf { it.isNotBlank() }
                ?.let(cipher::encrypt)
                ?: ""
        }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setThemeColor(id: String) {
        context.dataStore.edit { it[Keys.THEME_COLOR] = id }
    }
}
