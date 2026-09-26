package verlintas.openvisum.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
fun Modifier.pressScaleClickable(
    scaleDown: Float = 0.965f,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
        )
}

@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    maxDelayMs: Long = 260L,
    stepMs: Long = 35L,
): Modifier {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(10) * stepMs).coerceAtMost(maxDelayMs))
        appeared = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "entranceAlpha",
    )
    val offset by animateFloatAsState(
        targetValue = if (appeared) 0f else 32f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "entranceOffset",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offset
    }
}

@Composable
fun Modifier.popOnChange(trigger: Boolean, popScale: Float = 1.25f): Modifier {
    var target by remember { mutableStateOf(1f) }
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "popScale",
    )
    LaunchedEffect(trigger) {
        if (trigger) {
            target = popScale
            delay(140L)
            target = 1f
        }
    }
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
