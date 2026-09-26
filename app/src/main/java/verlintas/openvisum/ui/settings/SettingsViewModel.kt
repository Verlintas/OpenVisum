package verlintas.openvisum.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import verlintas.openvisum.AppLocale
import verlintas.openvisum.core.data.prefs.AppSettings
import verlintas.openvisum.core.data.prefs.PreferencesRepository

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val versionName: String = "",
)

class SettingsViewModel(
    private val context: android.content.Context,
    private val preferences: PreferencesRepository,
    private val versionName: String,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferences.settings
        .map { SettingsUiState(settings = it, versionName = versionName) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(versionName = versionName),
        )

    fun setHardwareDecoding(enabled: Boolean) {
        viewModelScope.launch { preferences.setHardwareDecoding(enabled) }
    }

    fun setDisableDirectRendering(enabled: Boolean) {
        viewModelScope.launch { preferences.setDisableDirectRendering(enabled) }
    }

    fun setAutoLoadExternalSubtitles(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoLoadExternalSubtitles(enabled) }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setThemeColor(id: String) {
        viewModelScope.launch { preferences.setThemeColor(id) }
    }

    fun setAppLanguage(tag: String) {
        viewModelScope.launch { preferences.setAppLanguage(tag) }
        runCatching { AppLocale.apply(context, tag) }
    }

    fun setDefaultPlaybackRate(rate: Float) {
        viewModelScope.launch { preferences.setDefaultPlaybackRate(rate) }
    }

    fun setRememberPlaybackPosition(enabled: Boolean) {
        viewModelScope.launch { preferences.setRememberPlaybackPosition(enabled) }
    }

    fun setSubtitleStyleDefaults(scale: Float, bold: Boolean, color: Int?) {
        viewModelScope.launch { preferences.setSubtitleStyle(scale, bold, color) }
    }

    fun setPreferredSubtitleLanguages(raw: String) {
        viewModelScope.launch {
            preferences.setPreferredSubtitleLanguages(parseLanguages(raw))
        }
    }

    fun setPreferredAudioLanguages(raw: String) {
        viewModelScope.launch {
            preferences.setPreferredAudioLanguages(parseLanguages(raw))
        }
    }

    fun setOpenSubtitlesCredentials(apiKey: String, username: String, password: String) {
        viewModelScope.launch {
            preferences.setOpenSubtitlesCredentials(apiKey.trim(), username.trim(), password)
        }
    }

    private fun parseLanguages(raw: String): List<String> =
        raw.split(',', '，', ' ', ';', '；')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    companion object {
        fun factory(
            context: android.content.Context,
            preferences: PreferencesRepository,
            versionName: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(context, preferences, versionName) as T
            }
        }
    }
}
