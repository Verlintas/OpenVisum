package verlintas.openvisum.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import verlintas.openvisum.R
import verlintas.openvisum.core.data.prefs.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SettingsCategoryRow(
                icon = Icons.Filled.PlayArrow,
                title = stringResource(R.string.settings_section_playback),
                summary = stringResource(
                    if (settings.hardwareDecoding) {
                        R.string.settings_summary_hardware_on
                    } else {
                        R.string.settings_summary_hardware_off
                    },
                ),
                onClick = onOpenPlayback,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsCategoryRow(
                icon = Icons.Filled.Subtitles,
                title = stringResource(R.string.settings_section_subtitles),
                summary = stringResource(
                    if (settings.autoLoadExternalSubtitles) {
                        R.string.settings_summary_autosub_on
                    } else {
                        R.string.settings_summary_autosub_off
                    },
                ),
                onClick = onOpenSubtitles,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsCategoryRow(
                icon = Icons.Filled.CloudDownload,
                title = stringResource(R.string.settings_section_online_subtitles),
                summary = stringResource(
                    if (settings.openSubtitlesApiKey.isNullOrBlank()) {
                        R.string.settings_summary_online_unconfigured
                    } else {
                        R.string.settings_summary_online_configured
                    },
                ),
                onClick = onOpenOnline,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsCategoryRow(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_section_appearance),
                summary = stringResource(
                    when (settings.themeMode) {
                        AppSettings.THEME_LIGHT -> R.string.settings_theme_light
                        AppSettings.THEME_DARK -> R.string.settings_theme_dark
                        else -> R.string.settings_theme_system
                    },
                ),
                onClick = onOpenAppearance,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsCategoryRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_section_about),
                summary = stringResource(R.string.settings_version, state.versionName),
                onClick = onOpenAbout,
            )
        }
    }
}

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

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}
