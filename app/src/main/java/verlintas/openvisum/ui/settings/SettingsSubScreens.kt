package verlintas.openvisum.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import verlintas.openvisum.R
import verlintas.openvisum.core.data.prefs.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsTopBar(title = title, onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    SettingsPage(
        title = stringResource(R.string.settings_section_playback),
        onBack = onBack,
    ) {
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            title = stringResource(R.string.settings_hardware_decoding),
            subtitle = stringResource(R.string.settings_hardware_decoding_hint),
            checked = settings.hardwareDecoding,
            onCheckedChange = viewModel::setHardwareDecoding,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SwitchRow(
            title = stringResource(R.string.settings_disable_direct_rendering),
            subtitle = stringResource(R.string.settings_disable_direct_rendering_hint),
            checked = settings.disableDirectRendering,
            onCheckedChange = viewModel::setDisableDirectRendering,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    var editingSubtitleLanguages by remember { mutableStateOf(false) }
    var editingAudioLanguages by remember { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(R.string.settings_section_subtitles),
        onBack = onBack,
    ) {
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            title = stringResource(R.string.settings_auto_subtitles),
            subtitle = stringResource(R.string.settings_auto_subtitles_hint),
            checked = settings.autoLoadExternalSubtitles,
            onCheckedChange = viewModel::setAutoLoadExternalSubtitles,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        ValueRow(
            title = stringResource(R.string.settings_preferred_subtitle_languages),
            value = settings.preferredSubtitleLanguages.joinToString(", "),
            onClick = { editingSubtitleLanguages = true },
        )
        HorizontalDivider()
        ValueRow(
            title = stringResource(R.string.settings_preferred_audio_languages),
            value = settings.preferredAudioLanguages.joinToString(", "),
            onClick = { editingAudioLanguages = true },
        )
    }

    if (editingSubtitleLanguages) {
        LanguageDialog(
            title = stringResource(R.string.settings_preferred_subtitle_languages),
            initial = settings.preferredSubtitleLanguages.joinToString(", "),
            onDismiss = { editingSubtitleLanguages = false },
            onConfirm = {
                viewModel.setPreferredSubtitleLanguages(it)
                editingSubtitleLanguages = false
            },
        )
    }

    if (editingAudioLanguages) {
        LanguageDialog(
            title = stringResource(R.string.settings_preferred_audio_languages),
            initial = settings.preferredAudioLanguages.joinToString(", "),
            onDismiss = { editingAudioLanguages = false },
            onConfirm = {
                viewModel.setPreferredAudioLanguages(it)
                editingAudioLanguages = false
            },
        )
    }
}

@Composable
fun OnlineSubtitleSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    var apiKey by remember(settings.openSubtitlesApiKey) {
        mutableStateOf(settings.openSubtitlesApiKey.orEmpty())
    }
    var username by remember(settings.openSubtitlesUsername) {
        mutableStateOf(settings.openSubtitlesUsername.orEmpty())
    }
    var password by remember(settings.openSubtitlesPassword) {
        mutableStateOf(settings.openSubtitlesPassword.orEmpty())
    }

    SettingsPage(
        title = stringResource(R.string.settings_section_online_subtitles),
        onBack = onBack,
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_opensubtitles_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.settings_opensubtitles_api_key)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.settings_opensubtitles_username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.settings_opensubtitles_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.setOpenSubtitlesCredentials(apiKey, username, password) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.common_save))
        }
    }
}

@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    SettingsPage(
        title = stringResource(R.string.settings_section_appearance),
        onBack = onBack,
    ) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_SYSTEM,
                onClick = { viewModel.setThemeMode(AppSettings.THEME_SYSTEM) },
                label = { Text(stringResource(R.string.settings_theme_system)) },
            )
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_LIGHT,
                onClick = { viewModel.setThemeMode(AppSettings.THEME_LIGHT) },
                label = { Text(stringResource(R.string.settings_theme_light)) },
            )
            FilterChip(
                selected = settings.themeMode == AppSettings.THEME_DARK,
                onClick = { viewModel.setThemeMode(AppSettings.THEME_DARK) },
                label = { Text(stringResource(R.string.settings_theme_dark)) },
            )
        }
    }
}

@Composable
fun AboutSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    SettingsPage(
        title = stringResource(R.string.settings_section_about),
        onBack = onBack,
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_version, state.versionName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_about_license),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_about_libvlc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_about_github),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                uriHandler.openUri("https://github.com/Verlintas/OpenVisum")
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguageDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
