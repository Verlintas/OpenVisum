package verlintas.openvisum.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import verlintas.openvisum.R
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.player.model.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    state: PlaybackState,
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

    val duration = state.durationMs.coerceAtLeast(1L)
    val displayedPosition = if (isDragging && sliderPosition >= 0f) {
        sliderPosition.toLong()
    } else {
        state.positionMs
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.75f),
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
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

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onSeekBy(-10_000L) },
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
                    .clickable { onSeekBy(10_000L) },
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
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
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                },
            )
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
                if (state.isBuffering) {
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
