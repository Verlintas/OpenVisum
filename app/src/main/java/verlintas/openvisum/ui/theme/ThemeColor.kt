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

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import verlintas.openvisum.R

enum class ThemeColor(
    val id: String,
    val seed: Long,
    val labelRes: Int,
) {
    DYNAMIC("dynamic", 0L, R.string.theme_color_dynamic),
    BRAND("brand", 0xFF4F6BED, R.string.theme_color_brand),
    TEAL("teal", 0xFF00838F, R.string.theme_color_teal),
    GREEN("green", 0xFF2E7D32, R.string.theme_color_green),
    AMBER("amber", 0xFFB26A00, R.string.theme_color_amber),
    ROSE("rose", 0xFFC2185B, R.string.theme_color_rose),
    VIOLET("violet", 0xFF6A3DE8, R.string.theme_color_violet),
    CRIMSON("crimson", 0xFFC62828, R.string.theme_color_crimson),
    SLATE("slate", 0xFF455A64, R.string.theme_color_slate),
    ;

    fun lightScheme(): ColorScheme = when (this) {
        DYNAMIC, BRAND -> brandLightScheme()
        TEAL -> tealLightScheme()
        GREEN -> greenLightScheme()
        AMBER -> amberLightScheme()
        ROSE -> roseLightScheme()
        VIOLET -> violetLightScheme()
        CRIMSON -> crimsonLightScheme()
        SLATE -> slateLightScheme()
    }

    fun darkScheme(): ColorScheme = when (this) {
        DYNAMIC, BRAND -> brandDarkScheme()
        TEAL -> tealDarkScheme()
        GREEN -> greenDarkScheme()
        AMBER -> amberDarkScheme()
        ROSE -> roseDarkScheme()
        VIOLET -> violetDarkScheme()
        CRIMSON -> crimsonDarkScheme()
        SLATE -> slateDarkScheme()
    }

    fun previewColor(): Color = Color(seed)

    companion object {
        val selectable: List<ThemeColor> = listOf(
            DYNAMIC, BRAND, TEAL, GREEN, AMBER, ROSE, VIOLET, CRIMSON, SLATE,
        )

        fun fromId(id: String?): ThemeColor =
            entries.firstOrNull { it.id == id } ?: BRAND
    }
}
