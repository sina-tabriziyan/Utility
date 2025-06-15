package com.sina.library.system.time

import kotlin.math.floor

class FileTimeConverter {
    private val gregorianEpoch = 1721425.5
    private val islamicEpoch = 1948439.5
    private val fileTimeOneDayS = 10000000L * 60
    private val fileTimeOneDayM = 10000000L * 60 * 60
    private val fileTimeOneDayH = 24L * 60 * 60 * 10000000
    private val oneSecond = 10000000
    private val oneMinute = 10000000L * 60
    private val oneHour = 10000000L * 60 * 60

    fun jdToDt(jd: Double, calendarType: Int): List<Double> {
        val dt = jdToGregorian(jd)
        val adjustedJd = gregorianToJd(dt[0], dt[1], dt[2])
        return when (calendarType) {
            0 -> jdToGregorian(adjustedJd)
            1 -> jdToPersian(adjustedJd)
            2 -> jdToIslamic(adjustedJd)
            else -> dt
        }
    }

    fun jdToGregorian(jd: Double): List<Double> {
        val wjd = floor(jd - 0.5) + 0.5
        val depoch = wjd - gregorianEpoch
        val quadricent = floor(depoch / 146097)
        val dqc = depoch % 146097
        val cent = floor(dqc / 36524)
        val dcent = dqc % 36524
        val quad = floor(dcent / 1461)
        val dquad = dcent % 1461
        val yindex = floor(dquad / 365)
        var year = quadricent * 400 + cent * 100 + quad * 4 + yindex
        if (!(cent.toInt() == 4 || yindex.toInt() == 4)) year++

        val yearday = wjd - gregorianToJd(year, 1.0, 1.0)
        val leapAdj = if (wjd < gregorianToJd(year, 3.0, 1.0)) 0.0 else if (leapGregorian(year)) 1.0 else 2.0
        val month = floor(((yearday + leapAdj) * 12 + 373) / 367)
        val day = (wjd - gregorianToJd(year, month, 1.0)) + 1

        return listOf(year, month, day)
    }

    fun jdToPersian(jd: Double): List<Double> {
        val adjustedJd = Math.floor(jd) + 0.5
        val depoch = adjustedJd - persianToJd(475.0, 1.0, 1.0)
        val cycle = Math.floor(depoch / 1029983)
        val cyear = depoch % 1029983
        val ycycle = if (cyear == 1029982.0) 2820.0 else {
            val aux1 = Math.floor(cyear / 366)
            val aux2 = cyear % 366
            Math.floor(((2134 * aux1) + (2816 * aux2) + 2815) / 1028522) + aux1 + 1
        }
        var year = ycycle + (2820 * cycle) + 474
        if (year <= 0) year--
        val yday = adjustedJd - persianToJd(year, 1.0, 1.0) + 1
        val month = if (yday <= 186) Math.ceil(yday / 31) else Math.ceil((yday - 6) / 30)
        val day = adjustedJd - persianToJd(year, month, 1.0) + 1

        return listOf(year, month, day)
    }

    fun jdToIslamic(jd: Double): List<Double> {
        val adjustedJd = Math.floor(jd) + 0.5
        val year = Math.floor(((30 * (adjustedJd - islamicEpoch)) + 10646) / 10631)
        val month = Math.min(12.0, Math.ceil((adjustedJd - (29 + islamicToJd(year, 1.0, 1.0))) / 29.5) + 1)
        val day = adjustedJd - islamicToJd(year, month, 1.0) + 1

        return listOf(year, month, day)
    }

    fun persianToJd(year: Double, month: Double, day: Double): Double {
        val epbase = year - if (year >= 0) 474 else 473
        val epyear = 474 + (epbase % 2820)
        return day + (if (month <= 7) (month - 1) * 31 else ((month - 1) * 30) + 6) +
                Math.floor(((epyear * 682) - 110) / 2816) +
                (epyear - 1) * 365 +
                Math.floor(epbase / 2820) * 1029983 +
                (1948320.5 - 1)
    }

    fun gregorianToJd(year: Double, month: Double, day: Double): Double {
        return (gregorianEpoch - 1) + (365 * (year - 1)) + Math.floor((year - 1) / 4) -
                Math.floor((year - 1) / 100) + Math.floor((year - 1) / 400) +
                Math.floor((((367 * month) - 362) / 12) + (if (month <= 2) 0 else if (leapGregorian(year)) -1 else -2) + day)
    }

    fun islamicToJd(year: Double, month: Double, day: Double): Double {
        return day + Math.ceil(29.5 * (month - 1)) + (year - 1) * 354 +
                Math.floor((3 + (11 * year)) / 30) + islamicEpoch - 1
    }

    fun filetimeToJd(filetime: Long, timezone: Long): Double {
        val filetimeZone = filetime + timezone
        return (((filetimeZone / 10000000) - 11644473600L) / 86400) + 2440587.5
    }

    fun filetimeModDay(filetime: Long): LongArray {
        val h = filetime % fileTimeOneDayH
        val m = h % fileTimeOneDayM
        val s = m % fileTimeOneDayS
        return longArrayOf(h, m, s)
    }

    fun leapGregorian(year: Double): Boolean {
        return ((year % 4) == 0.0) && !(((year % 100) == 0.0) && ((year % 400) != 0.0))
    }

    fun picker(filetime: Long, timezone: Long, daylightRange: String?, calendarType: Int): List<String> {
        var adjustedFiletime = filetime
        val jd = filetimeToJd(filetime, timezone)

        val dt = jdToDt(jd, calendarType)
        val filetimeMod = filetimeModDay(adjustedFiletime + timezone)

        return listOf(
            dt[0].toInt().toString(),
            dt[1].toInt().toString().padStart(2, '0'),
            dt[2].toInt().toString().padStart(2, '0'),
            (filetimeMod[0] / oneHour).toInt().toString().padStart(2, '0'),
            (filetimeMod[1] / oneMinute).toInt().toString().padStart(2, '0'),
            (filetimeMod[2] / oneSecond).toInt().toString().padStart(2, '0')
        )
    }
}
