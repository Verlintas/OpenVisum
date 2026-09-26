package verlintas.openvisum.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private const val THEME_TRANSITION_MS = 550

@Composable
fun animatedColorScheme(target: ColorScheme): ColorScheme {
    @Composable
    fun animate(targetColor: Color, label: String): Color = animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = THEME_TRANSITION_MS),
        label = label,
    ).value

    return target.copy(
        primary = animate(target.primary, "primary"),
        onPrimary = animate(target.onPrimary, "onPrimary"),
        primaryContainer = animate(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animate(target.onPrimaryContainer, "onPrimaryContainer"),
        inversePrimary = animate(target.inversePrimary, "inversePrimary"),
        secondary = animate(target.secondary, "secondary"),
        onSecondary = animate(target.onSecondary, "onSecondary"),
        secondaryContainer = animate(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animate(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animate(target.tertiary, "tertiary"),
        onTertiary = animate(target.onTertiary, "onTertiary"),
        tertiaryContainer = animate(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = animate(target.onTertiaryContainer, "onTertiaryContainer"),
        background = animate(target.background, "background"),
        onBackground = animate(target.onBackground, "onBackground"),
        surface = animate(target.surface, "surface"),
        onSurface = animate(target.onSurface, "onSurface"),
        surfaceVariant = animate(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = animate(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceContainerLowest = animate(target.surfaceContainerLowest, "surfaceContainerLowest"),
        surfaceContainerLow = animate(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = animate(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = animate(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = animate(target.surfaceContainerHighest, "surfaceContainerHighest"),
        surfaceBright = animate(target.surfaceBright, "surfaceBright"),
        surfaceDim = animate(target.surfaceDim, "surfaceDim"),
        outline = animate(target.outline, "outline"),
        outlineVariant = animate(target.outlineVariant, "outlineVariant"),
        error = animate(target.error, "error"),
        onError = animate(target.onError, "onError"),
        errorContainer = animate(target.errorContainer, "errorContainer"),
        onErrorContainer = animate(target.onErrorContainer, "onErrorContainer"),
        scrim = animate(target.scrim, "scrim"),
    )
}
