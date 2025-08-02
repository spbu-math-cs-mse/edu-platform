package com.github.heheteam.adminbot

import java.time.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

val dateFormatter: DateTimeFormat<LocalDate> =
  LocalDate.Format {
    dayOfMonth(padding = Padding.ZERO)
    char('.')
    monthNumber(padding = Padding.ZERO)
    char('.')
    year(padding = Padding.ZERO)
  }
val timeFormatter: DateTimeFormat<LocalTime> =
  LocalTime.Format {
    hour(padding = Padding.ZERO)
    char(':')
    minute(padding = Padding.ZERO)
  }

fun toRussian(d: DayOfWeek): String {
  return when (d.value) {
    1 -> "понедельник"
    2 -> "вторник"
    3 -> "среда"
    4 -> "четверг"
    5 -> "пятница"
    6 -> "суббота"
    else -> "воскресенье"
  }
}
