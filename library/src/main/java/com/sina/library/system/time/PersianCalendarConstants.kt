package com.sina.library.system.time

object PersianCalendarConstants {
    const val MILLIS_JULIAN_EPOCH: Long = -210866803200000L
    const val MILLIS_OF_A_DAY: Long = 86400000L
    const val PERSIAN_EPOCH: Long = 1948321
    val persianMonthNames = arrayOf(
        "\u0641\u0631\u0648\u0631\u062f\u06cc\u0646", // Farvardin
        "\u0627\u0631\u062f\u06cc\u0628\u0647\u0634\u062a", // Ordibehesht
        "\u062e\u0631\u062f\u0627\u062f", // Khordad
        "\u062a\u06cc\u0631", // Tir
        "\u0645\u0631\u062f\u0627\u062f", // Mordad
        "\u0634\u0647\u0631\u06cc\u0648\u0631", // Shahrivar
        "\u0645\u0647\u0631", // Mehr
        "\u0622\u0628\u0627\u0646", // Aban
        "\u0622\u0630\u0631", // Azar
        "\u062f\u06cc", // Dey
        "\u0628\u0647\u0645\u0646", // Bahman
        "\u0627\u0633\u0641\u0646\u062f" // Esfand
    )
    val persianWeekDays = arrayOf(
        "\u0634\u0646\u0628\u0647", // Shanbeh
        "\u06cc\u06a9\u200c\u0634\u0646\u0628\u0647", // Yekshanbeh
        "\u062f\u0648\u0634\u0646\u0628\u0647", // Doshanbeh
        "\u0633\u0647\u200c\u0634\u0646\u0628\u0647", // Sehshanbeh
        "\u0686\u0647\u0627\u0631\u0634\u0646\u0628\u0647", // Chaharshanbeh
        "\u067e\u0646\u062c\u200c\u0634\u0646\u0628\u0647", // Panjshanbeh
        "\u062c\u0645\u0639\u0647" // jome
    )
}