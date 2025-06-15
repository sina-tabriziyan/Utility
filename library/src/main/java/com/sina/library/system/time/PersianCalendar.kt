package com.sina.library.system.time

import android.content.Context
import java.util.GregorianCalendar
import java.util.TimeZone

class PersianCalendar : GregorianCalendar {
    private var context: Context? = null
    private var persianYear = 0
    private var persianMonth = 0
    private var persianDay = 0
    private var delimiter = "/"

    private fun convertToMilis(julianDate: Long): Long =
        PersianCalendarConstants.MILLIS_JULIAN_EPOCH + julianDate * PersianCalendarConstants.MILLIS_OF_A_DAY +
                ceil(
                    (timeInMillis - PersianCalendarConstants.MILLIS_JULIAN_EPOCH).toDouble(),
                    PersianCalendarConstants.MILLIS_OF_A_DAY.toDouble()
                )

    constructor(context: Context?, millis: Long) {
        this.context = context
        setTimeInMillis(millis)
    }

    constructor(millis: Long) {
        setTimeInMillis(millis)
    }

    constructor(context: Context?) {
        this.context = context
        timeZone = TimeZone.getTimeZone("GMT")
    }

    protected fun calculatePersianDate() {
        val julianDate =
            (Math.floor((timeInMillis - PersianCalendarConstants.MILLIS_JULIAN_EPOCH).toDouble()) / PersianCalendarConstants.MILLIS_OF_A_DAY).toLong()
        val PersianRowDate = julianToPersian(julianDate)
        val year = PersianRowDate shr 16
        persianYear = if (year > 0) year.toInt() else year.toInt() - 1
        persianMonth = (PersianRowDate and 0xff00 shr 8).toInt()
        persianDay = (PersianRowDate and 0xff).toInt()
    }

    fun isPersianLeapYear(): Boolean = isPersianLeapYear(persianYear)
    fun setPersianDate(persianYear: Int, persianMonth: Int, persianDay: Int) {
        this.persianYear = persianYear
        this.persianMonth = persianMonth + 1
        this.persianDay = persianDay
        setTimeInMillis(
            convertToMilis(
                persianToJulian(
                    (if (this.persianYear > 0) this.persianYear else this.persianYear + 1).toLong(),
                    this.persianMonth - 1,
                    this.persianDay
                )
            )
        )
    }

    fun getPersianYear(): Int = persianYear
    fun getPersianMonth(): Int = persianMonth
    fun getPersianMonthName(): String = context!!.resources.getStringArray(com.sina.library.utility.R.array.month_name)[persianMonth]
    fun getPersianDay(): Int = persianDay
    fun getPersianWeekDayName(): String {
        return when (get(DAY_OF_WEEK)) {
            SATURDAY -> PersianCalendarConstants.persianWeekDays[0]
            SUNDAY -> PersianCalendarConstants.persianWeekDays[1]
            MONDAY -> PersianCalendarConstants.persianWeekDays[2]
            TUESDAY -> PersianCalendarConstants.persianWeekDays[3]
            WEDNESDAY -> PersianCalendarConstants.persianWeekDays[4]
            THURSDAY -> PersianCalendarConstants.persianWeekDays[5]
            else -> PersianCalendarConstants.persianWeekDays[6]
        }
    }

    fun getPersianLongDate(): String =
        getPersianWeekDayName() + "  " + persianDay + "  " + getPersianMonthName() + "  " + persianYear

    fun getPersianLongDateAndTime(): String =
        getPersianLongDate() + " ساعت " + get(HOUR_OF_DAY) + ":" + get(MINUTE) + ":" + get(SECOND)

    fun getPersianShortTime(): String =
        formatToMilitary(get(HOUR_OF_DAY)) + ":" + formatToMilitary(get(MINUTE))

    fun getPersianShortDate(): String =
        "" + formatToMilitary(persianYear) + delimiter + formatToMilitary(getPersianMonth() + 1) + delimiter + formatToMilitary(
            persianDay
        )

    private fun formatToMilitary(number: Int): String = if (number < 10) "0$number" else number.toString()
    fun addPersianDate(field: Int, amount: Int) {
        if (amount == 0) return
        if (field < 0 || field >= ZONE_OFFSET) throw IllegalArgumentException()
        if (field == YEAR) {
            setPersianDate(persianYear + amount, getPersianMonth() + 1, persianDay)
            return
        } else if (field == MONTH) {
            val newYear = persianYear + (getPersianMonth() + 1 + amount) / 12
            val newMonth = (getPersianMonth() + 1 + amount) % 12
            setPersianDate(newYear, newMonth, persianDay)
            return
        }
        add(field, amount)
        calculatePersianDate()
    }

    fun parse(dateString: String) {
        val p = context?.let { PersianDateParser(dateString, delimiter).getPersianDate(it) }
        if (p != null) setPersianDate(p.persianYear, p.persianMonth, p.persianDay)
    }

    fun getDelimiter(): String = delimiter
    fun setDelimiter(delimiter: String) {
        this.delimiter = delimiter
    }

    override fun toString(): String {
        val str = super.toString()
        return "${str.substring(0, str.length - 1)},PersianDate=${getPersianShortDate()}]"
    }

    override fun equals(other: Any?): Boolean = super.equals(other)
    override fun hashCode(): Int = super.hashCode()
    override fun set(field: Int, value: Int) {
        super.set(field, value)
        calculatePersianDate()
    }

    override fun setTimeInMillis(millis: Long) {
        super.setTimeInMillis(millis)
        calculatePersianDate()
    }

    override fun setTimeZone(zone: TimeZone) {
        super.setTimeZone(zone)
        calculatePersianDate()
    }
}
