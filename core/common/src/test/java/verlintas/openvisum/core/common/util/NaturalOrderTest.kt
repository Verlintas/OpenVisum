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

package verlintas.openvisum.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {

    private fun sorted(names: List<String>): List<String> = names.sortedWith(NaturalOrder)

    @Test
    fun `numeric runs compare numerically`() {
        assertEquals(
            listOf("Ep 1.mkv", "Ep 2.mkv", "Ep 10.mkv"),
            sorted(listOf("Ep 10.mkv", "Ep 2.mkv", "Ep 1.mkv")),
        )
    }

    @Test
    fun `case is ignored`() {
        assertEquals(
            listOf("alpha.mkv", "Beta.mkv"),
            sorted(listOf("Beta.mkv", "alpha.mkv")),
        )
    }

    @Test
    fun `leading zeros are handled`() {
        assertEquals(
            listOf("E01.mp4", "E2.mp4", "E10.mp4"),
            sorted(listOf("E10.mp4", "E2.mp4", "E01.mp4")),
        )
    }

    @Test
    fun `plain suffix sorts before numeric suffix`() {
        assertTrue(NaturalOrder.compare("movie.mp4", "movie2.mp4") < 0)
        assertTrue(NaturalOrder.compare("movie part 2.mp4", "movie part 10.mp4") < 0)
    }

    @Test
    fun `chinese and latin mixed names keep stable order`() {
        val names = listOf("第10集.mp4", "第2集.mp4", "第1集.mp4")
        assertEquals(listOf("第1集.mp4", "第2集.mp4", "第10集.mp4"), sorted(names))
    }
}
