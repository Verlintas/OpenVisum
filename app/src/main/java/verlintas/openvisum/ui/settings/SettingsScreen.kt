/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

package verlintas.openvisum.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import verlintas.openvisum.diagnostics.DiagnosticReport
import verlintas.openvisum.ui.components.LocalAppSnackbar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import verlintas.openvisum.R
import verlintas.openvisum.core.data.prefs.AppSettings
import verlintas.openvisum.ui.components.SettingsGroup
import verlintas.openvisum.ui.components.SettingsToggleRow
import verlintas.openvisum.ui.components.SettingsGroupDivider
import verlintas.openvisum.ui.components.SettingsNavRow
import verlintas.openvisum.ui.components.staggeredEntrance
import verlintas.openvisum.ui.theme.ThemeColor

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
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val exportLogsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val report = DiagnosticReport.build(context, state.versionName)
        val saved = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(report.toByteArray())
                true
            } ?: false
        }.getOrDefault(false)
        scope.launch {
            snackbar?.showSnackbar(
                context.getString(
                    if (saved) R.string.settings_export_logs_done else R.string.settings_export_logs_failed,
                ),
            )
        }
    }

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
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
            Spacer(Modifier.height(8.dp))
            SettingsGroup(modifier = Modifier.staggeredEntrance(index = 0)) {
                SettingsNavRow(
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
                SettingsGroupDivider()
                SettingsNavRow(
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
                SettingsGroupDivider()
                SettingsNavRow(
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
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_section_appearance),
                    summary = buildString {
                        append(
                            stringResource(
                                when (settings.themeMode) {
                                    AppSettings.THEME_LIGHT -> R.string.settings_theme_light
                                    AppSettings.THEME_DARK -> R.string.settings_theme_dark
                                    else -> R.string.settings_theme_system
                                },
                            ),
                        )
                        append(" · ")
                        append(
                            stringResource(
                                ThemeColor.fromId(settings.themeColor).labelRes,
                            ),
                        )
                    },
                    onClick = onOpenAppearance,
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    icon = Icons.Filled.VisibilityOff,
                    title = stringResource(R.string.settings_hide_content_title),
                    subtitle = stringResource(R.string.settings_hide_content_summary),
                    checked = settings.hideContentOnLaunch,
                    onCheckedChange = viewModel::setHideContentOnLaunch,
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    icon = Icons.Filled.Cast,
                    title = stringResource(R.string.settings_dlna_receiver_title),
                    subtitle = if (settings.dlnaReceiverEnabled) {
                        stringResource(R.string.settings_dlna_receiver_active, android.os.Build.MODEL)
                    } else {
                        stringResource(R.string.settings_dlna_receiver_summary)
                    },
                    checked = settings.dlnaReceiverEnabled,
                    onCheckedChange = viewModel::setDlnaReceiverEnabled,
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.settings_export_logs_title),
                    summary = stringResource(R.string.settings_export_logs_summary),
                    onClick = { exportLogsLauncher.launch(DiagnosticReport.suggestedFileName()) },
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_section_about),
                    summary = stringResource(R.string.settings_version, state.versionName),
                    onClick = onOpenAbout,
                )
            }
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}
