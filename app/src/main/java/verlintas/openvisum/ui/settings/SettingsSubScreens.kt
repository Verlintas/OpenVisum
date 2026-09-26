package verlintas.openvisum.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import verlintas.openvisum.R
import verlintas.openvisum.core.data.prefs.AppSettings
import verlintas.openvisum.ui.components.SettingsGroup
import verlintas.openvisum.ui.components.SettingsGroupDivider
import verlintas.openvisum.ui.components.SettingsGroupLabel
import verlintas.openvisum.ui.components.SettingsHintText
import verlintas.openvisum.ui.components.SettingsToggleRow
import verlintas.openvisum.ui.components.SettingsValueRow
import verlintas.openvisum.ui.theme.ThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                )
            }
        },
    )
}

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
            Spacer(Modifier.height(8.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

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
        SettingsGroup {
            SettingsToggleRow(
                icon = Icons.Filled.Memory,
                title = stringResource(R.string.settings_hardware_decoding),
                subtitle = stringResource(R.string.settings_hardware_decoding_hint),
                checked = settings.hardwareDecoding,
                onCheckedChange = viewModel::setHardwareDecoding,
            )
            SettingsGroupDivider()
            SettingsToggleRow(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.settings_disable_direct_rendering),
                subtitle = stringResource(R.string.settings_disable_direct_rendering_hint),
                checked = settings.disableDirectRendering,
                onCheckedChange = viewModel::setDisableDirectRendering,
            )
        }
    }
}

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
        SettingsGroup {
            SettingsToggleRow(
                icon = Icons.Filled.ClosedCaption,
                title = stringResource(R.string.settings_auto_subtitles),
                subtitle = stringResource(R.string.settings_auto_subtitles_hint),
                checked = settings.autoLoadExternalSubtitles,
                onCheckedChange = viewModel::setAutoLoadExternalSubtitles,
            )
        }
        SettingsGroupLabel(stringResource(R.string.settings_group_languages))
        SettingsGroup {
            SettingsValueRow(
                icon = Icons.Filled.Subtitles,
                title = stringResource(R.string.settings_preferred_subtitle_languages),
                value = settings.preferredSubtitleLanguages.joinToString(", "),
                onClick = { editingSubtitleLanguages = true },
            )
            SettingsGroupDivider()
            SettingsValueRow(
                icon = Icons.Filled.Translate,
                title = stringResource(R.string.settings_preferred_audio_languages),
                value = settings.preferredAudioLanguages.joinToString(", "),
                onClick = { editingAudioLanguages = true },
            )
        }
        SettingsHintText(stringResource(R.string.settings_language_hint))
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
        SettingsHintText(stringResource(R.string.settings_opensubtitles_hint))
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            Column(modifier = Modifier.padding(16.dp)) {
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
            }
        }
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
        SettingsGroupLabel(stringResource(R.string.settings_theme_mode))
        SettingsGroup {
            Column(modifier = Modifier.padding(16.dp)) {
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

        SettingsGroupLabel(stringResource(R.string.settings_theme_color))
        SettingsGroup {
            ThemeColorPicker(
                selectedId = settings.themeColor,
                onSelect = viewModel::setThemeColor,
            )
        }
    }
}

@Composable
private fun ThemeColorPicker(
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ThemeColor.selectable.chunked(4).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowColors.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = color.id == selectedId,
                        onClick = { onSelect(color.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowColors.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = color.previewColor()
    val brush: Brush = if (color == ThemeColor.DYNAMIC) {
        Brush.sweepGradient(
            listOf(
                Color(0xFFF44336), Color(0xFFFF9800), Color(0xFFFFEB3B),
                Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFF3F51B5),
                Color(0xFF9C27B0), Color(0xFFF44336),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                color.darkScheme().primary,
                color.lightScheme().primaryContainer,
            ),
        )
    }
    val useDarkCheck = preview.luminance() > 0.45f && color != ThemeColor.DYNAMIC

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(brush)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (useDarkCheck) Color.Black.copy(alpha = 0.75f) else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(color.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
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
        SettingsGroup {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.settings_version, state.versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
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
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/Verlintas/OpenVisum")
                    },
                )
            }
        }
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
