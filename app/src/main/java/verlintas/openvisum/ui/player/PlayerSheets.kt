package verlintas.openvisum.ui.player

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.LanguageUtils
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.data.subtitle.SubtitleSearchResult
import verlintas.openvisum.core.player.equalizer.EqualizerPresets
import verlintas.openvisum.core.player.model.AudioStereoMode
import verlintas.openvisum.core.player.model.EqualizerState
import verlintas.openvisum.core.player.model.PlaybackState
import verlintas.openvisum.core.player.model.PlayerTrack
import verlintas.openvisum.core.player.model.RendererDevice
import verlintas.openvisum.core.player.model.SubtitleStyle
import verlintas.openvisum.core.player.model.TrackType
import verlintas.openvisum.core.player.model.VideoScaleMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun TrackRow(
    track: PlayerTrack,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val subtitle = buildList {
        LanguageUtils.displayName(track.language)?.let { add(it) }
        track.codec?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        track.channels?.let { add(if (it == 1) "Mono" else "$it ch") }
        track.sampleRate?.let { add("${it / 1000} kHz") }
        track.width?.takeIf { it > 0 }?.let { width ->
            track.height?.let { height -> add("$width×$height") }
        }
        track.frameRate?.let { add("%.3f fps".format(it)) }
        if (track.external) add(stringResource(R.string.player_external))
    }.joinToString(" · ")

    ListItem(
        headlineContent = {
            Text(
                text = track.name.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.player_track_default_name, track.id),
            )
        },
        supportingContent = if (subtitle.isBlank()) null else {
            { Text(subtitle, style = MaterialTheme.typography.bodySmall) }
        },
        trailingContent = {
            val checkScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "trackCheck",
            )
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                    alpha = checkScale
                },
            )
        },
        modifier = Modifier.clickable(onClick = onSelect),
    )
}

@Composable
fun AudioTrackSheet(
    state: PlaybackState,
    onSelect: (Int) -> Unit,
    onDelayChange: (Long) -> Unit,
    onStereoModeChange: (AudioStereoMode) -> Unit,
    onPassthroughChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBottomSheet(
        title = stringResource(R.string.player_audio_tracks),
        onDismiss = onDismiss,
    ) {
        if (state.audioTracks.isEmpty()) {
            EmptyTracksHint()
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                items(state.audioTracks) { track ->
                    TrackRow(
                        track = track,
                        selected = track.id == state.selectedAudioTrackId,
                        onSelect = { onSelect(track.id) },
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DelaySlider(
            label = stringResource(R.string.player_audio_delay),
            delayMs = state.audioDelayMs,
            onDelayChange = onDelayChange,
        )
        StereoModeSelector(
            current = state.stereoMode,
            onSelect = onStereoModeChange,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.player_passthrough),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        if (state.passthroughAvailable) {
                            R.string.player_passthrough_hint
                        } else {
                            R.string.player_passthrough_unavailable
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.audioDigitalOutput,
                onCheckedChange = onPassthroughChange,
                enabled = state.passthroughAvailable,
            )
        }
    }
}

@Composable
private fun StereoModeSelector(
    current: AudioStereoMode,
    onSelect: (AudioStereoMode) -> Unit,
) {
    val modes = listOf(
        AudioStereoMode.AUTO to stringResource(R.string.stereo_auto),
        AudioStereoMode.STEREO to stringResource(R.string.stereo_stereo),
        AudioStereoMode.REVERSE_STEREO to stringResource(R.string.stereo_reverse),
        AudioStereoMode.LEFT to stringResource(R.string.stereo_left),
        AudioStereoMode.RIGHT to stringResource(R.string.stereo_right),
        AudioStereoMode.DOLBY to stringResource(R.string.stereo_dolby),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.player_stereo_mode),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(modes) { (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = if (mode == current) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    modifier = Modifier.clickable { onSelect(mode) },
                )
            }
        }
    }
}

@Composable
fun SubtitleTrackSheet(
    state: PlaybackState,
    onSelect: (Int) -> Unit,
    onAddSubtitle: () -> Unit,
    onOnlineSearch: () -> Unit,
    onDelayChange: (Long) -> Unit,
    onStyleChange: (SubtitleStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBottomSheet(
        title = stringResource(R.string.player_subtitle_tracks),
        onDismiss = onDismiss,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.common_off)) },
            trailingContent = if (state.selectedSubtitleTrackId == PlayerTrack.TRACK_DISABLED) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else {
                null
            },
            modifier = Modifier.clickable { onSelect(PlayerTrack.TRACK_DISABLED) },
        )
        if (state.subtitleTracks.isEmpty()) {
            EmptyTracksHint()
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(state.subtitleTracks) { track ->
                    if (track.id != PlayerTrack.TRACK_DISABLED) {
                        TrackRow(
                            track = track,
                            selected = track.id == state.selectedSubtitleTrackId,
                            onSelect = { onSelect(track.id) },
                        )
                    }
                }
            }
        }
        TextButton(onClick = onAddSubtitle) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.player_add_subtitle))
        }
        TextButton(onClick = onOnlineSearch) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.player_online_subtitles))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DelaySlider(
            label = stringResource(R.string.player_subtitle_delay),
            delayMs = state.subtitleDelayMs,
            onDelayChange = onDelayChange,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SubtitleStyleSection(
            style = state.subtitleStyle,
            onStyleChange = onStyleChange,
        )
    }
}

@Composable
private fun SubtitleStyleSection(
    style: SubtitleStyle,
    onStyleChange: (SubtitleStyle) -> Unit,
) {
    var scale by remember(style.textScale) { mutableFloatStateOf(style.textScale) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.player_subtitle_style),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.player_subtitle_scale), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "%.1fx".format(scale),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = scale,
            onValueChange = { scale = it },
            onValueChangeFinished = { onStyleChange(style.copy(textScale = scale)) },
            valueRange = SubtitleStyle.MIN_SCALE..SubtitleStyle.MAX_SCALE,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.player_subtitle_bold),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = style.bold,
                onCheckedChange = { onStyleChange(style.copy(bold = it)) },
            )
        }
        Text(
            text = stringResource(R.string.player_subtitle_color),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = style.color == null,
                onClick = { onStyleChange(style.copy(color = null)) },
                label = { Text(stringResource(R.string.subtitle_color_default)) },
            )
            FilterChip(
                selected = style.color == 0xFFFFFF,
                onClick = { onStyleChange(style.copy(color = 0xFFFFFF)) },
                label = { Text(stringResource(R.string.subtitle_color_white)) },
            )
            FilterChip(
                selected = style.color == 0xFFFF00,
                onClick = { onStyleChange(style.copy(color = 0xFFFF00)) },
                label = { Text(stringResource(R.string.subtitle_color_yellow)) },
            )
            FilterChip(
                selected = style.color == 0x00FFFF,
                onClick = { onStyleChange(style.copy(color = 0x00FFFF)) },
                label = { Text(stringResource(R.string.subtitle_color_cyan)) },
            )
            FilterChip(
                selected = style.color == 0x00FF00,
                onClick = { onStyleChange(style.copy(color = 0x00FF00)) },
                label = { Text(stringResource(R.string.subtitle_color_green)) },
            )
        }
    }
}

@Composable
private fun EmptyTracksHint() {
    Text(
        text = stringResource(R.string.player_no_tracks),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun DelaySlider(
    label: String,
    delayMs: Long,
    onDelayChange: (Long) -> Unit,
) {
    var sliderValue by remember(delayMs) { mutableFloatStateOf(delayMs.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "%+d ms".format(sliderValue.toLong()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onDelayChange(sliderValue.toLong()) },
            valueRange = -10_000f..10_000f,
        )
    }
}

@Composable
fun SpeedSheet(
    state: PlaybackState,
    onSelect: (Float) -> Unit,
    onSetAbStart: () -> Unit,
    onSetAbEnd: () -> Unit,
    onClearAb: () -> Unit,
    onDismiss: () -> Unit,
) {
    val rates = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f)
    TrackBottomSheet(
        title = stringResource(R.string.player_speed),
        onDismiss = onDismiss,
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(rates) { rate ->
                ListItem(
                    headlineContent = { Text("%.2fx".format(rate)) },
                    trailingContent = if (rate == state.rate) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    modifier = Modifier.clickable { onSelect(rate) },
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(R.string.player_ab_loop),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onSetAbStart, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.player_ab_set_a))
            }
            TextButton(
                onClick = onSetAbEnd,
                enabled = state.abLoopStartMs != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.player_ab_set_b))
            }
            TextButton(
                onClick = onClearAb,
                enabled = state.abLoopStartMs != null || state.abLoopEndMs != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.player_ab_clear))
            }
        }
        Text(
            text = stringResource(
                R.string.player_ab_range,
                state.abLoopStartMs?.let(TimeUtils::formatDuration) ?: "--:--",
                state.abLoopEndMs?.let(TimeUtils::formatDuration) ?: "--:--",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
fun CastSheet(
    active: RendererDevice?,
    devices: List<RendererDevice>,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBottomSheet(
        title = stringResource(R.string.player_cast),
        onDismiss = onDismiss,
    ) {
        if (active != null) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.player_cast_disconnect, active.name)) },
                leadingContent = { Icon(Icons.Filled.CastConnected, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onDisconnect),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        if (devices.isEmpty()) {
            Text(
                text = stringResource(R.string.player_cast_searching),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            devices.forEach { device ->
                ListItem(
                    headlineContent = { Text(device.name) },
                    supportingContent = { Text(device.type.uppercase()) },
                    leadingContent = { Icon(Icons.Filled.Cast, contentDescription = null) },
                    modifier = Modifier.clickable { onConnect(device.id) },
                )
            }
        }
    }
}

@Composable
fun AspectSheet(
    state: PlaybackState,
    onSelectScale: (VideoScaleMode) -> Unit,
    onRotate: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBottomSheet(
        title = stringResource(R.string.player_aspect),
        onDismiss = onDismiss,
    ) {
        val modes = listOf(
            VideoScaleMode.FIT_SCREEN to stringResource(R.string.aspect_fit_screen),
            VideoScaleMode.FILL_SCREEN to stringResource(R.string.aspect_fill_screen),
            VideoScaleMode.ORIGINAL to stringResource(R.string.aspect_original),
            VideoScaleMode.RATIO_16_9 to stringResource(R.string.aspect_16_9),
            VideoScaleMode.RATIO_4_3 to stringResource(R.string.aspect_4_3),
            VideoScaleMode.RATIO_21_9 to stringResource(R.string.aspect_21_9),
            VideoScaleMode.RATIO_235_1 to stringResource(R.string.aspect_235_1),
        )
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            items(modes) { (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = if (mode == state.videoScale) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    modifier = Modifier.clickable { onSelectScale(mode) },
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.player_rotate),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(listOf(0, 90, 180, 270)) { degrees ->
                ListItem(
                    headlineContent = { Text("$degrees°") },
                    trailingContent = if (degrees == state.rotationDegrees) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    modifier = Modifier.clickable { onRotate(degrees) },
                )
            }
        }
    }
}

@Composable
fun OnlineSubtitleSheet(
    state: OnlineSubtitleState,
    initialQuery: String,
    onSearch: (String) -> Unit,
    onDownload: (SubtitleSearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    TrackBottomSheet(
        title = stringResource(R.string.player_online_subtitles),
        onDismiss = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.player_online_search_hint)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onSearch(query.trim()) },
                enabled = query.isNotBlank() && !state.isSearching,
            ) {
                Text(stringResource(R.string.player_online_search))
            }
        }
        if (state.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(state.results, key = { "${it.providerId}-${it.id}" }) { result ->
                ListItem(
                    headlineContent = {
                        Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Text(
                            buildString {
                                append(LanguageUtils.displayName(result.language) ?: result.language)
                                result.format?.let { append(" · ${it.uppercase()}") }
                                result.downloads?.let { append(" · ${it} downloads") }
                            },
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Subtitles, contentDescription = null)
                    },
                    trailingContent = {
                        if (state.downloadingId == result.id) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Filled.Download, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable(enabled = state.downloadingId == null) {
                        onDownload(result)
                    },
                )
            }
        }
    }
}

@Composable
fun EqualizerSheet(
    state: PlaybackState,
    onApply: (EqualizerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val presetNames = remember { EqualizerPresets.presetNames }
    var enabled by remember(state.equalizer.enabled) { mutableStateOf(state.equalizer.enabled) }
    var presetIndex by remember(state.equalizer.presetIndex) {
        mutableStateOf(state.equalizer.presetIndex)
    }
    var preamp by remember(state.equalizer.preamp) { mutableFloatStateOf(state.equalizer.preamp) }
    var bands by remember(state.equalizer.bandAmplitudes) {
        mutableStateOf(
            state.equalizer.bandAmplitudes.ifEmpty {
                EqualizerPresets.amplitudesForPreset(state.equalizer.presetIndex)
            },
        )
    }

    fun apply() {
        onApply(
            EqualizerState(
                enabled = enabled,
                presetIndex = presetIndex,
                preamp = preamp,
                bandAmplitudes = bands,
            ),
        )
    }

    TrackBottomSheet(
        title = stringResource(R.string.player_equalizer),
        onDismiss = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.eq_enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    apply()
                },
            )
        }

        if (enabled) {
            Text(
                text = stringResource(R.string.eq_preset),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                items(presetNames) { name ->
                    val index = presetNames.indexOf(name)
                    ListItem(
                        headlineContent = { Text(name.ifBlank { "Preset $index" }) },
                        trailingContent = if (index == presetIndex) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        modifier = Modifier.clickable {
                            presetIndex = index
                            bands = EqualizerPresets.amplitudesForPreset(index)
                            apply()
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.eq_preamp), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "%+.1f dB".format(preamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = preamp,
                onValueChange = { preamp = it },
                onValueChangeFinished = { apply() },
                valueRange = -20f..20f,
            )

            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(bands.size) { band ->
                    val frequency = EqualizerPresets.bandFrequency(band)
                    val label = if (frequency >= 1000f) {
                        "%.1f kHz".format(frequency / 1000f)
                    } else {
                        "%.0f Hz".format(frequency)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(64.dp),
                        )
                        Slider(
                            value = bands[band],
                            onValueChange = { value ->
                                bands = bands.toMutableList().also { it[band] = value }
                            },
                            onValueChangeFinished = { apply() },
                            valueRange = -20f..20f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "%+.1f".format(bands[band]),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(48.dp),
                        )
                    }
                }
            }
        }
    }
}
