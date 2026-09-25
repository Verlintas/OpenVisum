package verlintas.openvisum.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class SortOrder { DATE_DESC, NAME_ASC, SIZE_DESC, DURATION_DESC }

data class AppSettings(
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val hardwareDecoding: Boolean = true,
    val preferredSubtitleLanguages: List<String> = defaultSubtitleLanguages(),
    val preferredAudioLanguages: List<String> = defaultAudioLanguages(),
    val autoLoadExternalSubtitles: Boolean = true,
    val subtitleTextSize: Int = 0,
    val themeMode: Int = THEME_SYSTEM,
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

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        val PREFERRED_SUBTITLE_LANGUAGES = stringSetPreferencesKey("preferred_subtitle_languages")
        val PREFERRED_AUDIO_LANGUAGES = stringSetPreferencesKey("preferred_audio_languages")
        val AUTO_LOAD_EXTERNAL_SUBTITLES = booleanPreferencesKey("auto_load_external_subtitles")
        val SUBTITLE_TEXT_SIZE = intPreferencesKey("subtitle_text_size")
        val THEME_MODE = intPreferencesKey("theme_mode")
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
                preferredSubtitleLanguages = preferences[Keys.PREFERRED_SUBTITLE_LANGUAGES]?.toList()
                    ?: defaultSubtitleLanguages(),
                preferredAudioLanguages = preferences[Keys.PREFERRED_AUDIO_LANGUAGES]?.toList()
                    ?: defaultAudioLanguages(),
                autoLoadExternalSubtitles = preferences[Keys.AUTO_LOAD_EXTERNAL_SUBTITLES] ?: true,
                subtitleTextSize = preferences[Keys.SUBTITLE_TEXT_SIZE] ?: 0,
                themeMode = preferences[Keys.THEME_MODE] ?: AppSettings.THEME_SYSTEM,
            )
        }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }
    }

    suspend fun setHardwareDecoding(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_DECODING] = enabled }
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

    suspend fun setSubtitleTextSize(size: Int) {
        context.dataStore.edit { it[Keys.SUBTITLE_TEXT_SIZE] = size }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }
}
