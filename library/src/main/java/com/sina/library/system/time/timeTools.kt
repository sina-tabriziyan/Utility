/**
 * Created by ST on 1/22/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.system.time

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date


fun Context.currentTime(): String = DateFormat.format("kk:mm", Calendar.getInstance().time) as String

fun currentTimeMillisToDate(currentTimeMillis: Long): Date = Date(currentTimeMillis)

fun convertToWindowsFileTime(time: Long): Long = (time + 11644473600L) * 10000000

fun millisToUnixTime(millis: Long): Long = millis / 1000