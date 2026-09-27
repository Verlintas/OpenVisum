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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import verlintas.openvisum.R

private val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold),
)

private val Base = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Manrope),
        displayMedium = base.displayMedium.copy(fontFamily = Manrope),
        displaySmall = base.displaySmall.copy(fontFamily = Manrope),
        headlineLarge = base.headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = base.headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = base.headlineSmall.copy(fontFamily = Manrope),
        titleLarge = base.titleLarge.copy(fontFamily = Manrope),
        titleMedium = base.titleMedium.copy(fontFamily = Manrope),
        titleSmall = base.titleSmall.copy(fontFamily = Manrope),
        bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
        bodySmall = base.bodySmall.copy(fontFamily = Manrope),
        labelLarge = base.labelLarge.copy(fontFamily = Manrope),
        labelMedium = base.labelMedium.copy(fontFamily = Manrope),
        labelSmall = base.labelSmall.copy(fontFamily = Manrope),
    )
}

val OpenVisumTypography: Typography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = Base.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = Base.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = Base.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = Base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = Base.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = Base.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
    bodyLarge = Base.bodyLarge.copy(
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = Base.bodyMedium.copy(
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
    ),
)

val OpenVisumShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)
