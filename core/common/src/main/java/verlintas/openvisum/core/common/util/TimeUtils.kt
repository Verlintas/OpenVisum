package verlintas.openvisum.core.common.util

object TimeUtils {

    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formatDurationWithMillis(ms: Long): String {
        if (ms <= 0L) return "00:00.000"
        val totalMillis = ms
        val hours = totalMillis / 3_600_000
        val minutes = (totalMillis % 3_600_000) / 60_000
        val seconds = (totalMillis % 60_000) / 1000
        val millis = totalMillis % 1000
        return if (hours > 0) {
            "%d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
        } else {
            "%02d:%02d.%03d".format(minutes, seconds, millis)
        }
    }
}
