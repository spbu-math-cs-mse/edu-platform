package com.github.heheteam.adminbot

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
