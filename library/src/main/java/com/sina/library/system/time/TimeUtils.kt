package com.sina.library.system.time

import android.annotation.SuppressLint
import java.util.concurrent.TimeUnit
object TimeUtils {

    /**
     * Formats a time duration given in milliseconds into a string format MM:SS or HH:MM:SS.
     *
     * @param millis The duration in milliseconds.
     * @return A string representing the formatted time. Returns "00:00" if millis is negative.
     */
    @SuppressLint("DefaultLocale")
    fun formatDuration(millis: Long): String {
        if (millis < 0) {
            // Or throw an IllegalArgumentException, or return a specific error string
            return "00:00"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                      TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats a time duration given in integer milliseconds into a string format MM:SS or HH:MM:SS.
     *
     * @param millisInt The duration in milliseconds as an Int.
     * @return A string representing the formatted time.
     */
    fun formatDuration(millisInt: Int): String {
        return formatDuration(millisInt.toLong())
    }
}
