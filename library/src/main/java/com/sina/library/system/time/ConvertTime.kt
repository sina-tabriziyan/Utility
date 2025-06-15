package com.sina.library.system.time

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.sina.library.utility.R

import java.text.DateFormat
import java.text.DateFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
object ConvertTime {

    // --- Public Facing Methods ---

    @JvmStatic
    fun getModifiedTime(
        context: Context,
        fileTime: String?,
        shortTimeOnly: Boolean,
        timeZoneIdStr: String?, // IANA Time Zone ID (e.g., "America/New_York")
        dayLightRange: String?,
        calendarType: Int?,
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getModifiedTimeApi26(context, fileTime, shortTimeOnly, timeZoneIdStr, dayLightRange, calendarType)
        } else {
            getModifiedTimeLegacy(context, fileTime, shortTimeOnly, timeZoneIdStr, dayLightRange, calendarType)
        }
    }

    @JvmStatic
    fun getDate(
        context: Context,
        fileTime: String,
        language: String, // e.g., "fa", "en"
        timezoneOffsetMillis: Long // This is an offset from UTC in millis for the source fileTime
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getDateApi26(context, fileTime, language, timezoneOffsetMillis)
        } else {
            getDateLegacy(context, fileTime, language, timezoneOffsetMillis)
        }
    }


    // --- API 26+ Implementation (using java.time) ---

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getModifiedTimeApi26(
        context: Context,
        fileTime: String?,
        shortTimeOnly: Boolean,
        timeZoneIdStr: String?,
        dayLightRange: String?, // Your custom param
        calendarType: Int?
    ): String {
        if (fileTime == "-1") return " "

        val sourceTimeMillis = fileTime?.toLongOrNull()
            ?: convertToWindowsFileTime(System.currentTimeMillis()) // Assuming this is fallback to current time in your FT format

        // Convert Windows File Time to Epoch Millis (UTC)
        // This logic needs to be robust. Assuming fileTimeToNormalTime WITHOUT offset gives UTC millis.
        val epochMillisUtc = fileTimeToNormalTime(sourceTimeMillis.toString()) // Ensure this gives clean UTC epoch ms

        if (epochMillisUtc == 0L && sourceTimeMillis != 0L) return " " // Error in conversion

        val instant = Instant.ofEpochMilli(epochMillisUtc)
        val targetZoneId = timeZoneIdStr?.let {
            try { ZoneId.of(it) } catch (e: Exception) { ZoneId.systemDefault() }
        } ?: ZoneId.systemDefault()
        val zonedDateTime = instant.atZone(targetZoneId)


        return if (shortTimeOnly) {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .format(zonedDateTime)
        } else {
            when {
                isToday(zonedDateTime) -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                    .format(zonedDateTime)

                calendarType == 0 -> { // Gregorian-like
                    if (zonedDateTime.year == ZonedDateTime.now(targetZoneId).year) {
                        // "Month Day" for current year. Using a pattern.
                        // For true locale-awareness, Android ICU DateTimePatternGenerator is better but more complex.
                        DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()).format(zonedDateTime)
                    } else {
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                            .withLocale(Locale.getDefault())
                            .format(zonedDateTime)
                    }
                }
                else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                    .format(zonedDateTime)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateApi26(
        context: Context,
        fileTime: String,
        language: String,
        timezoneOffsetMillis: Long // Offset from UTC for the *source* fileTime
    ): String {
        // Convert the source fileTime (which is at a given offset) to UTC epoch milliseconds
        val epochMillisUtc = fileTimeToNormalTime(fileTime, timezoneOffsetMillis, isOffsetAppliedToSource = true)
        if (epochMillisUtc == 0L && fileTime != "0") return " "

        val instant = Instant.ofEpochMilli(epochMillisUtc)
        // For display, use the device's default timezone
        val displayZoneId = ZoneId.systemDefault()
        val zonedDateTime = instant.atZone(displayZoneId)

        if (language.equals("fa", ignoreCase = true) || language.equals("Persian", ignoreCase = true)) {
            val persianCalendar = PersianCalendar(context, epochMillisUtc) // PersianCalendar should take UTC epoch ms
            return if (isToday(persianCalendar, context, System.currentTimeMillis())) { // Compare with current time
                context.getString(R.string.txt_today)
            } else {
                // Your existing Persian formatting
                StringBuilder()
                    .append(ConvertNumber.convertEnToFa(persianCalendar.getPersianYear().toString()))
                    .append("/")
                    .append(persianCalendar.getPersianMonthName())
                    .append("/")
                    .append(ConvertNumber.convertEnToFa(persianCalendar.getPersianDay().toString()))
                    .toString()
            }
        } else { // Default to Gregorian with device locale
            return if (isToday(zonedDateTime)) {
                context.getString(R.string.txt_today)
            } else {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) // e.g., Jan 12, 2023
                    .withLocale(Locale.getDefault())
                    .format(zonedDateTime)
            }
        }
    }


    // --- Legacy API Implementation (< API 26, using java.text & java.util) ---

    private fun getModifiedTimeLegacy(
        context: Context,
        fileTime: String?,
        shortTimeOnly: Boolean,
        timeZoneIdStr: String?,
        dayLightRange: String?, // Your custom param
        calendarType: Int?
    ): String {
        if (fileTime == "-1") return " "

        val sourceTimeMillis = fileTime?.toLongOrNull()
            ?: convertToWindowsFileTime(System.currentTimeMillis())

        val epochMillisUtc = fileTimeToNormalTime(sourceTimeMillis.toString())
        if (epochMillisUtc == 0L && sourceTimeMillis != 0L) return " "

        val targetTimeZone = timeZoneIdStr?.let {
            try { TimeZone.getTimeZone(it) } catch (e: Exception) { TimeZone.getDefault() }
        } ?: TimeZone.getDefault()

        val calendar = Calendar.getInstance(targetTimeZone, Locale.getDefault())
        calendar.timeInMillis = epochMillisUtc

        return if (shortTimeOnly) {
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
            timeFormat.timeZone = targetTimeZone
            timeFormat.format(calendar.time)
        } else {
            when {
                isToday(calendar) -> { // Use Calendar based isToday
                    val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                    timeFormat.timeZone = targetTimeZone
                    timeFormat.format(calendar.time)
                }
                calendarType == 0 -> { // Gregorian-like
                    val todayCal = Calendar.getInstance(targetTimeZone, Locale.getDefault())
                    if (calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)) {
                        val monthName = DateFormatSymbols(Locale.getDefault()).months[calendar.get(Calendar.MONTH)]
                        "$monthName ${calendar.get(Calendar.DAY_OF_MONTH)}"
                    } else {
                        val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
                        dateFormat.timeZone = targetTimeZone
                        dateFormat.format(calendar.time)
                    }
                }
                else -> {
                    val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
                    dateFormat.timeZone = targetTimeZone
                    dateFormat.format(calendar.time)
                }
            }
        }
    }

    private fun getDateLegacy(
        context: Context,
        fileTime: String,
        language: String,
        timezoneOffsetMillis: Long // Offset from UTC for the *source* fileTime
    ): String {
        val epochMillisUtc = fileTimeToNormalTime(fileTime, timezoneOffsetMillis, isOffsetAppliedToSource = true)
        if (epochMillisUtc == 0L && fileTime != "0") return " "

        // For display, use the device's default timezone
        val displayTimeZone = TimeZone.getDefault()
        val calendar = Calendar.getInstance(displayTimeZone, Locale.getDefault())
        calendar.timeInMillis = epochMillisUtc

        if (language.equals("fa", ignoreCase = true) || language.equals("Persian", ignoreCase = true)) {
            val persianCalendar = PersianCalendar(context, epochMillisUtc)
            return if (isToday(persianCalendar, context, System.currentTimeMillis())) {
                context.getString(R.string.txt_today)
            } else {
                StringBuilder()
                    .append(ConvertNumber.convertEnToFa(persianCalendar.getPersianYear().toString()))
                    .append("/")
                    .append(persianCalendar.getPersianMonthName())
                    .append("/")
                    .append(ConvertNumber.convertEnToFa(persianCalendar.getPersianDay().toString()))
                    .toString()
            }
        } else { // Default to Gregorian with device locale
            return if (isToday(calendar)) {
                context.getString(R.string.txt_today)
            } else {
                val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                dateFormat.timeZone = displayTimeZone
                dateFormat.format(calendar.time)
            }
        }
    }


    // --- Common Helper Methods ---

    // isToday for ZonedDateTime (API 26+)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isToday(zonedDateTime: ZonedDateTime): Boolean {
        val todayInZone = ZonedDateTime.now(zonedDateTime.zone)
        return zonedDateTime.toLocalDate().isEqual(todayInZone.toLocalDate())
    }

    // isToday for Calendar (Legacy and common)
    private fun isToday(calendar: Calendar): Boolean {
        val todayCalendar = Calendar.getInstance(calendar.timeZone, Locale.getDefault())
        return calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
    }

    // isToday for PersianCalendar (Your existing logic, ensure it's correct)
    private fun isToday(
        persianCalendar: PersianCalendar,
        context: Context,
        currentTimeMillisForToday: Long
    ): Boolean {
        val todayPersianCalendar = PersianCalendar(context, currentTimeMillisForToday)
        return persianCalendar.getPersianYear() == todayPersianCalendar.getPersianYear() &&
                persianCalendar.getPersianMonth() == todayPersianCalendar.getPersianMonth() &&
                persianCalendar.getPersianDay() == todayPersianCalendar.getPersianDay()
    }

    // --- FileTime Conversion Logic (Crucial: Ensure this correctly handles your specific "fileTime" format) ---
    // This is a critical piece. It needs to convert your proprietary 'fileTime' string
    // (and potentially an offset) into standard UTC Epoch Milliseconds.

    /**
     * Converts a Windows File Time string to UTC Epoch milliseconds.
     * @param fileTimeStr The file time as a string.
     * @return UTC Epoch milliseconds, or 0L on error or invalid input.
     */
    private fun fileTimeToNormalTime(fileTimeStr: String): Long { // Assumes fileTimeStr is UTC if no offset given
        return fileTimeToNormalTime(fileTimeStr, 0L, isOffsetAppliedToSource = false)
    }

    /**
     * Converts a Windows File Time string to UTC Epoch milliseconds, potentially adjusting for a source offset.
     * @param fileTimeStr The file time as a string.
     * @param offsetMillis The offset in milliseconds.
     * @param isOffsetAppliedToSource If true, offsetMillis is subtracted to get to UTC.
     *                                If false, offsetMillis is added (less common for epoch conversion).
     * @return UTC Epoch milliseconds, or 0L on error.
     */
    private fun fileTimeToNormalTime(fileTimeStr: String, offsetMillis: Long, isOffsetAppliedToSource: Boolean): Long {
        return try {
            val fileTimeLong = fileTimeStr.toLongOrNull() ?: return 0L

            // Windows File Time is 100-nanosecond intervals since January 1, 1601 (UTC).
            // Constant for seconds between Windows epoch (1601-01-01) and Unix epoch (1970-01-01)
            val secondsBetweenEpochs = 11644473600L

            if (fileTimeLong < 0) return 0L // Or some other minimum check specific to your fileTime format

            // Convert fileTime (100-ns intervals) to seconds since Windows epoch
            val secondsSinceWindowsEpoch = fileTimeLong / 10_000_000L

            // Convert to seconds since Unix epoch (UTC)
            var unixSecondsUtc = secondsSinceWindowsEpoch - secondsBetweenEpochs

            // If the provided offsetMillis was for the *source* time, adjust to get to UTC
            if (isOffsetAppliedToSource) {
                unixSecondsUtc -= (offsetMillis / 1000L)
            }

            unixSecondsUtc * 1000L // Convert to milliseconds
        } catch (e: NumberFormatException) {
            0L
        }
    }


    // Your existing Windows File Time utilities (if needed by your Picker logic or as fallback)
    private const val WINDOWS_TICK_FACTOR = 10000000L // 100-nanosecond units
    private const val SEC_TO_UNIX_EPOCH = 11644473600L

    /**
     * Converts Unix epoch milliseconds to your Windows File Time format (as Long).
     */
    private fun convertToWindowsFileTime(unixMillis: Long): Long {
        if (unixMillis < 0) return 0L // Or handle as error
        val unixSeconds = unixMillis / 1000L
        return (unixSeconds + SEC_TO_UNIX_EPOCH) * WINDOWS_TICK_FACTOR
    }

    // Placeholder for your FileTimeConvertor, assuming it's external or complex
    // If FileTimeConvertor.Picker is used by the *legacy* path, it needs to work with Calendar/TimeZone
    private class FileTimeConvertor {
        // Example: Picker might need to be adapted or its output re-interpreted for legacy path
        fun Picker(
            fileTimeRepresentation: Long, // Your specific file time number
            timezoneOffsetForPicker: Long, // Raw offset in ms, or similar concept
            dayLightRange: String?,
            mode: Int
        ): Array<String> {
            // This is highly dependent on your FileTimeConvertor's internal logic.
            // It needs to return [yearStr, monthStr(1-12), dayStr, hourStr, minuteStr, secondStr]
            // based on its inputs. For the legacy path, it should ideally use the
            // timezoneOffsetForPicker to produce components already in that target timezone.
            //
            // Simplified Example (replace with your actual logic):
            // This example assumes fileTimeRepresentation is epoch millis UTC and timezoneOffsetForPicker
            // is the offset to apply to get components in the target zone.
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = fileTimeRepresentation + timezoneOffsetForPicker // Apply offset

            return arrayOf(
                cal.get(Calendar.YEAR).toString(),
                (cal.get(Calendar.MONTH) + 1).toString(), // Month is 0-indexed in Calendar
                cal.get(Calendar.DAY_OF_MONTH).toString(),
                cal.get(Calendar.HOUR_OF_DAY).toString(),
                cal.get(Calendar.MINUTE).toString(),
                cal.get(Calendar.SECOND).toString()
            )
        }
    }
}
