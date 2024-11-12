package com.github.heheteam.adminbot

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun toRussian(d: DayOfWeek): String {
    when (d.getValue()) {
        1 -> return "понедельник"
        2 -> return "вторник"
        3 -> return "среда"
        4 -> return "четверг"
        5 -> return "пятница"
        6 -> return "суббота"
        else -> return "воскресенье"
    }
}