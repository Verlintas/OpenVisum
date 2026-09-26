package verlintas.openvisum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import verlintas.openvisum.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.videoFrameMillis
import verlintas.openvisum.core.common.util.FileSizeUtils
import verlintas.openvisum.core.common.util.TimeUtils
import verlintas.openvisum.core.data.model.MediaItem

@Composable
fun MediaGridCard(
    item: MediaItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    entranceIndex: Int = 0,
) {
    val progress by animateFloatAsState(
        targetValue = if (item.playbackDurationMs > 0L) {
            (item.playbackPositionMs.toFloat() / item.playbackDurationMs).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 650),
        label = "cardProgress",
    )
    Column(
        modifier = modifier
            .staggeredEntrance(entranceIndex)
            .clip(RoundedCornerShape(18.dp))
            .pressScaleClickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            val frame = if (item.durationMs > 0L) {
                (item.durationMs / 4).coerceIn(5_000L, 120_000L)
            } else {
                5_000L
            }
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .videoFrameMillis(frame)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { ThumbnailPlaceholder() },
                error = { ThumbnailPlaceholder() },
                modifier = Modifier.matchParentSize(),
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.62f),
                        ),
                    ),
            )

            val watched = item.playbackDurationMs > 0L &&
                item.playbackPositionMs.toFloat() / item.playbackDurationMs >= 0.95f
            if (watched) {
                Text(
                    text = stringResource(R.string.library_badge_watched),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            } else if (item.playbackPositionMs > 0L && item.playbackDurationMs > 0L) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                resolutionBadge(item)?.let { CardBadge(it) }
                containerBadge(item)?.let { CardBadge(it, subtle = true) }
            }

            if (item.durationMs > 0L) {
                Text(
                    text = TimeUtils.formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.32f)
                        },
                    )
                    .clickable(onClick = onToggleFavorite)
                    .popOnChange(trigger = item.isFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (item.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = null,
                    tint = if (item.isFavorite) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        Color.White
                    },
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            text = buildString {
                if (item.sizeBytes > 0) append(FileSizeUtils.formatSize(item.sizeBytes))
                item.folderName?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}


@Composable
private fun CardBadge(text: String, subtle: Boolean = false) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (subtle) {
                    Color.Black.copy(alpha = 0.45f)
                } else {
                    Color.Black.copy(alpha = 0.65f)
                },
            )
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

internal fun resolutionBadge(item: MediaItem): String? = when {
    item.height >= 2000 -> "4K"
    item.height >= 1000 -> "1080P"
    item.height >= 700 -> "720P"
    item.height > 0 -> "${item.height}P"
    else -> null
}

internal fun containerBadge(item: MediaItem): String? {
    val mime = item.mimeType.orEmpty()
    return when {
        mime.contains("matroska") -> "MKV"
        mime.contains("mp4") -> "MP4"
        mime.contains("webm") -> "WEBM"
        mime.contains("x-msvideo") || mime.contains("avi") -> "AVI"
        mime.contains("quicktime") -> "MOV"
        mime.contains("mpeg") -> "MPEG"
        else -> item.displayName
            ?.substringAfterLast('.', "")
            ?.uppercase()
            ?.takeIf { it.length in 2..4 }
    }
}
