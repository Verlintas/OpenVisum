package verlintas.openvisum.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil3.video.videoFrameMillis
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.player.model.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    state: PlaybackState,
    visible: Boolean,
    isCasting: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onOpenCast: () -> Unit,
) {
    var sliderPosition by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val duration = state.durationMs.coerceAtLeast(1L)
    val displayedPosition = if (isDragging && sliderPosition >= 0f) {
        sliderPosition.toLong()
    } else {
        state.positionMs
    }
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 26.dp else 18.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumbSize",
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220),
        label = "scrimAlpha",
    )

    var rateIndicator by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(state.rate) {
        if (state.rate != 1f) {
            rateIndicator = state.rate
            delay(1100L)
            rateIndicator = null
        }
    }

    var seekIndicator by remember { mutableLongStateOf(0L) }
    var seekIndicatorKey by remember { mutableStateOf<Long?>(null) }
    fun triggerSeekIndicator(deltaMs: Long) {
        seekIndicator = deltaMs
        seekIndicatorKey = System.currentTimeMillis()
        scope.launch {
            delay(750L)
            seekIndicatorKey = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.68f * scrimAlpha),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f * scrimAlpha),
                        ),
                    ),
                ),
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 3 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = Color.White,
                    )
                }
                Text(
                    text = state.title.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenCast) {
                    Icon(
                        imageVector = if (isCasting) Icons.Filled.CastConnected else Icons.Filled.Cast,
                        contentDescription = stringResource(R.string.player_cast),
                        tint = if (isCasting) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
                if (state.videoWidth > 0 && state.videoHeight > 0) {
                    Text(
                        text = "${state.videoWidth}×${state.videoHeight}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(240), initialScale = 0.86f),
            exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.86f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            triggerSeekIndicator(-10_000L)
                            onSeekBy(-10_000L)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = stringResource(R.string.player_back_10s),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.94f),
                        contentColor = Color(0xFF101014),
                    ),
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                ) {
                    AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.6f) + fadeIn()).togetherWith(
                                scaleOut(targetScale = 0.6f) + fadeOut(),
                            )
                        },
                        label = "playPauseIcon",
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (playing) R.string.player_pause else R.string.player_play,
                            ),
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            triggerSeekIndicator(10_000L)
                            onSeekBy(10_000L)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = stringResource(R.string.player_forward_10s),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 3 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fullWidth = maxWidth
                    val previewFraction = (displayedPosition.toFloat() / duration)
                        .coerceIn(0f, 1f)
                    val previewWidth = 168.dp
                    val previewOffset = (fullWidth * previewFraction - previewWidth / 2)
                        .coerceIn(0.dp, (fullWidth - previewWidth).coerceAtLeast(0.dp))
                    Slider(
                        value = displayedPosition.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { value ->
                            isDragging = true
                            sliderPosition = value
                        },
                        onValueChangeFinished = {
                            if (sliderPosition >= 0f) onSeek(sliderPosition.toLong())
                            isDragging = false
                            sliderPosition = -1f
                        },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        ),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(thumbSize)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        },
                    )
                    if (isDragging && state.mediaUri != null) {
                        val previewAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(120),
                            label = "previewAlpha",
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = previewOffset, y = (-118).dp)
                                .graphicsLayer {
                                    alpha = previewAlpha
                                    scaleX = 0.97f + 0.03f * previewAlpha
                                    scaleY = 0.97f + 0.03f * previewAlpha
                                },
                        ) {
                            SeekPreview(
                                uri = state.mediaUri.toString(),
                                positionMs = displayedPosition,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = TimeUtils.formatDuration(displayedPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    AnimatedVisibility(
                        visible = state.isBuffering,
                        enter = fadeIn(tween(160)),
                        exit = fadeOut(tween(160)),
                    ) {
                        Text(
                            text = stringResource(R.string.player_buffering),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        text = TimeUtils.formatDuration(state.durationMs),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlButton(
                        icon = {
                            Text(
                                text = "%.2fx".format(state.rate),
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                        label = stringResource(R.string.player_speed),
                        onClick = { onOpenSheet(PlayerSheet.SPEED) },
                    )
                    ControlButton(
                        icon = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                        },
                        label = stringResource(R.string.player_audio),
                        onClick = { onOpenSheet(PlayerSheet.AUDIO) },
                    )
                    ControlButton(
                        icon = {
                            Icon(Icons.Filled.Subtitles, contentDescription = null, tint = Color.White)
                        },
                        label = stringResource(R.string.player_subtitle),
                        onClick = { onOpenSheet(PlayerSheet.SUBTITLE) },
                    )
                    ControlButton(
                        icon = {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color.White)
                        },
                        label = stringResource(R.string.player_equalizer),
                        onClick = { onOpenSheet(PlayerSheet.EQUALIZER) },
                    )
                    ControlButton(
                        icon = {
                            Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = Color.White)
                        },
                        label = stringResource(R.string.player_aspect),
                        onClick = { onOpenSheet(PlayerSheet.ASPECT) },
                    )
                    ControlButton(
                        icon = {
                            Icon(Icons.Filled.ScreenRotation, contentDescription = null, tint = Color.White)
                        },
                        label = stringResource(R.string.player_rotate),
                        onClick = { onOpenSheet(PlayerSheet.ASPECT) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = rateIndicator != null,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(220)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
        ) {
            StatusPill(text = "%.2fx".format(rateIndicator ?: state.rate))
        }

        AnimatedVisibility(
            visible = seekIndicatorKey != null,
            enter = fadeIn(tween(140)) + scaleIn(tween(220), initialScale = 0.8f),
            exit = fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (seekIndicator < 0) {
                        Icons.Filled.Replay10
                    } else {
                        Icons.Filled.Forward10
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (seekIndicator < 0) "-10s" else "+10s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        AnimatedVisibility(
            visible = state.isBuffering && !visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.player_buffering),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun ControlButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}


@Composable
private fun SeekPreview(
    uri: String,
    positionMs: Long,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bucket = (positionMs / PREVIEW_BUCKET_MS) * PREVIEW_BUCKET_MS
    val request = remember(uri, bucket) {
        coil3.request.ImageRequest.Builder(context)
            .data(uri)
            .videoFrameMillis(bucket)
            .size(336, 188)
            .memoryCacheKey("seek-preview-" + uri + "-" + bucket)
            .build()
    }
    val painter = coil3.compose.rememberAsyncImagePainter(model = request)
    val painterState by painter.state.collectAsState()
    val lastFrame = remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }
    androidx.compose.runtime.LaunchedEffect(painterState) {
        if (painterState is coil3.compose.AsyncImagePainter.State.Success) {
            lastFrame.value = painter
        }
    }
    val displayPainter = if (painterState is coil3.compose.AsyncImagePainter.State.Success) {
        painter
    } else {
        lastFrame.value
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.18f),
            ),
        ) {
            Box(
                modifier = Modifier
                    .width(168.dp)
                    .height(94.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (displayPainter != null) {
                    Image(
                        painter = displayPainter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF15161A)),
                    )
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = TimeUtils.formatDuration(positionMs),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private const val PREVIEW_BUCKET_MS = 10_000L
