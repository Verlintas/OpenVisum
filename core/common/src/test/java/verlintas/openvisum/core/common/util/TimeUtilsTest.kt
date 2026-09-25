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
