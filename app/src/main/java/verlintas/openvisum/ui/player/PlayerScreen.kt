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

package verlintas.openvisum.ui.player

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.videolan.libvlc.util.VLCVideoLayout
import verlintas.openvisum.OpenVisumApp
import verlintas.openvisum.core.player.model.VideoScaleMode

@Composable
fun PlayerScreen(
    mediaUri: String,
    mediaTitle: String?,
    onBack: () -> Unit,
    restart: Boolean = false,
) {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as OpenVisumApp).container
    }
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.factory(
            engine = container.playbackEngine,
            repository = container.mediaRepository,
            networkRepository = container.networkRepository,
            subtitleSearchRepository = container.subtitleSearchRepository,
            preferences = container.preferencesRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onlineSubtitles by viewModel.onlineSubtitles.collectAsStateWithLifecycle()
    val renderers by viewModel.renderers.collectAsStateWithLifecycle()
    val activeRenderer by viewModel.activeRenderer.collectAsStateWithLifecycle()
    val uri = remember(mediaUri) { Uri.parse(mediaUri) }

    LaunchedEffect(uri, mediaTitle, restart) {
        viewModel.openIfNeeded(uri, mediaTitle, restart)
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = generateSequence(view.context as android.content.Context?) { context ->
            (context as? android.content.ContextWrapper)?.baseContext
        }.firstOrNull { it is android.app.Activity } as? android.app.Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var activeSheet by remember { mutableStateOf<PlayerSheet?>(null) }

    LaunchedEffect(state.isPlaying, controlsVisible) {
        if (controlsVisible && state.isPlaying) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    LaunchedEffect(activeSheet) {
        if (activeSheet == PlayerSheet.CAST) {
            viewModel.startRendererDiscovery()
        } else {
            viewModel.stopRendererDiscovery()
        }
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { picked ->
        picked?.let(viewModel::addSubtitleFile)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    if (activeSheet == null) controlsVisible = !controlsVisible
                }
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            val containerWidth = maxWidth.value
            val containerHeight = maxHeight.value
            val videoAspect = if (state.videoWidth > 0 && state.videoHeight > 0) {
                state.videoWidth.toFloat() / state.videoHeight.toFloat()
            } else {
                0f
            }
            val cropFill = state.videoScale == VideoScaleMode.CROP_FILL && videoAspect > 0f
            val layerScale = if (cropFill) {
                val fitWidth: Float
                val fitHeight: Float
                if (containerWidth / containerHeight > videoAspect) {
                    fitHeight = containerHeight
                    fitWidth = containerHeight * videoAspect
                } else {
                    fitWidth = containerWidth
                    fitHeight = containerWidth / videoAspect
                }
                maxOf(containerWidth / fitWidth, containerHeight / fitHeight)
            } else {
                1f
            }
            AndroidView(
                factory = { ctx ->
                    VLCVideoLayout(ctx).apply {
                        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                            viewModel.onSurfaceLayoutChanged()
                        }
                        viewModel.attach(this)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = layerScale
                        scaleY = layerScale
                    },
            )
        }

        PlayerControls(
            state = state,
            visible = controlsVisible,
            isCasting = activeRenderer != null,
            onBack = onBack,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSeek = viewModel::seekTo,
            onSeekBy = viewModel::seekBy,
            onOpenSheet = { activeSheet = it },
            onOpenCast = { activeSheet = PlayerSheet.CAST },
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
            )
        }
    }

    when (activeSheet) {
        PlayerSheet.AUDIO -> AudioTrackSheet(
            state = state,
            onSelect = viewModel::selectAudioTrack,
            onDelayChange = viewModel::setAudioDelay,
            onStereoModeChange = viewModel::setStereoMode,
            onPassthroughChange = viewModel::setAudioDigitalOutput,
            onDismiss = { activeSheet = null },
        )

        PlayerSheet.SUBTITLE -> SubtitleTrackSheet(
            state = state,
            onSelect = viewModel::selectSubtitleTrack,
            onAddSubtitle = { subtitlePicker.launch(arrayOf("*/*")) },
            onOnlineSearch = { activeSheet = PlayerSheet.ONLINE_SUBTITLE },
            onDelayChange = viewModel::setSubtitleDelay,
            onStyleChange = viewModel::setSubtitleStyle,
            onDismiss = { activeSheet = null },
        )

        PlayerSheet.ONLINE_SUBTITLE -> OnlineSubtitleSheet(
            state = onlineSubtitles,
            initialQuery = mediaTitle ?: state.title.orEmpty(),
            onSearch = viewModel::searchOnlineSubtitles,
            onDownload = viewModel::downloadSubtitle,
            onDismiss = { activeSheet = null },
        )

        PlayerSheet.SPEED -> SpeedSheet(
            state = state,
            onSelect = viewModel::setRate,
            onSetAbStart = viewModel::markAbLoopStart,
            onSetAbEnd = viewModel::markAbLoopEnd,
            onClearAb = viewModel::clearAbLoop,
            onDismiss = { activeSheet = null },
        )

        PlayerSheet.CAST -> CastSheet(
            active = activeRenderer,
            devices = renderers,
            onRescan = viewModel::rescanRenderers,
            onConnect = viewModel::connectRenderer,
            onDisconnect = viewModel::disconnectRenderer,
            onDismiss = { activeSheet = null },
        )

        PlayerSheet.ASPECT -> AspectSheet(
            state = state,
            onSelectScale = viewModel::setVideoScale,
            onRotate = viewModel::setRotation,
            onDismiss = { activeSheet = null },
        )

        PlayerSheet.EQUALIZER -> EqualizerSheet(
            state = state,
            onApply = viewModel::setEqualizer,
            onDismiss = { activeSheet = null },
        )

        null -> Unit
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgressNow()
            viewModel.detach()
        }
    }
}

private const val CONTROLS_AUTO_HIDE_MS = 4000L
