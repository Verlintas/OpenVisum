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
