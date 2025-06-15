/**
 * Created by ST on 1/22/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.system.time

fun persianToJulian(year: Long, month: Int, day: Int): Long {
    return 365L * ((ceil((year - 474L).toDouble(), 2820.0) + 474L) - 1L) +
            ((Math.floor((682L * (ceil((year - 474L).toDouble(), 2820.0) + 474L) - 110L) / 2816.0)).toLong()) +
            (PersianCalendarConstants.PERSIAN_EPOCH - 1L) +
            1029983L * (Math.floor((year - 474L) / 2820.0)).toLong() +
            (if (month < 7) 31 * month else 30 * month + 6) + day
}
fun isPersianLeapYear(persianYear: Int): Boolean = ceil((38.0 + (ceil((persianYear - 474L).toDouble(), 2820.0) + 474L)) * 682.0, 2816.0) < 682L
fun julianToPersian(julianDate: Long): Long {
    val persianEpochInJulian = julianDate - persianToJulian(475L, 0, 1)
    val cyear = ceil(persianEpochInJulian.toDouble(), 1029983.0)
    val ycycle = if (cyear != 1029982L) Math.floor((2816.0 * cyear + 1031337.0) / 1028522.0).toLong() else 2820L
    val year = 474L + 2820L * Math.floor(persianEpochInJulian.toDouble() / 1029983.0).toLong() + ycycle
    val aux = (1L + julianDate) - persianToJulian(year, 0, 1)
    val month = if (aux > 186L) Math.ceil((aux - 6L) / 30.0).toInt() - 1 else Math.ceil(aux / 31.0).toInt() - 1
    val day = (julianDate - (persianToJulian(year, month, 1) - 1L)).toInt()
    return (year shl 16) or ((month shl 8).toLong()) or day.toLong()
}
fun ceil(double1: Double, double2: Double): Long = (double1 - double2 * Math.floor(double1 / double2)).toLong()
fun compare(persianCalendar1: PersianCalendar, persianCalendar2: PersianCalendar): Int {
    val date1 = persianCalendar1.getPersianYear() * 10000 + persianCalendar1.getPersianMonth() * 100 + persianCalendar1.getPersianDay()
    val date2 = persianCalendar2.getPersianYear() * 10000 + persianCalendar2.getPersianMonth() * 100 + persianCalendar2.getPersianDay()
    return date1.compareTo(date2)
}