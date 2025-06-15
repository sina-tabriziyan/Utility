package com.sina.library.system.time

import android.content.Context

class PersianDateParser(private var dateString: String) {
    private var delimiter: String = "/"

    constructor(dateString: String, delimiter: String) : this(dateString) { this.delimiter = delimiter }

    fun getPersianDate(context: Context): PersianCalendar {
        checkDateStringInitialValidation()
        val tokens = splitDateString(normalizeDateString(dateString))
        val year = tokens[0].toInt()
        val month = tokens[1].toInt()
        val day = tokens[2].toInt()
        checkPersianDateValidation(year, month, day)
        val pCal = PersianCalendar(context)
        pCal.setPersianDate(year, month, day)
        return pCal
    }

    private fun checkPersianDateValidation(year: Int, month: Int, day: Int) {
        if (year < 1)
            throw RuntimeException("year is not valid")
        if (month < 1 || month > 12)
            throw RuntimeException("month is not valid")
        if (day < 1 || day > 31)
            throw RuntimeException("day is not valid")
        if (month > 6 && day == 31)
            throw RuntimeException("day is not valid")
        if (month == 12 && day == 30 && !isPersianLeapYear(year))
            throw RuntimeException("day is not valid $year is not a leap year")
    }

    private fun normalizeDateString(dateString: String): String = dateString
    private fun splitDateString(dateString: String): Array<String> {
        val tokens = dateString.split(delimiter.toRegex()).toTypedArray()
        if (tokens.size != 3) throw RuntimeException("wrong date:$dateString is not a Persian Date or can not be parsed")
        return tokens
    }
    private fun checkDateStringInitialValidation() { if (dateString == null) throw RuntimeException("input didn't assing please use setDateString()") }
    fun getDateString(): String = dateString
    fun setDateString(dateString: String) { this.dateString = dateString }
    fun getDelimiter(): String = delimiter
    fun setDelimiter(delimiter: String) { this.delimiter = delimiter }
}
