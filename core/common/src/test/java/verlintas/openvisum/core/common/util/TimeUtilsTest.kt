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
import org.junit.Assert.assertNull
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun formatsShortDurations() {
        assertEquals("00:00", TimeUtils.formatDuration(0))
        assertEquals("00:05", TimeUtils.formatDuration(5_000))
        assertEquals("01:30", TimeUtils.formatDuration(90_000))
    }

    @Test
    fun formatsLongDurations() {
        assertEquals("1:00:00", TimeUtils.formatDuration(3_600_000))
        assertEquals("2:03:04", TimeUtils.formatDuration(7_384_000))
    }

    @Test
    fun formatsNegativeAsZero() {
        assertEquals("00:00", TimeUtils.formatDuration(-5))
    }
}
