package com.github.heheteam.commonlib.util

import korlibs.time.fromMinutes
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private const val MIDDAY: Int = 12
private const val JANUARY: Int = 1
private const val YEAR: Int = 2024

private val timeZone = TimeZone.of("Europe/Moscow")

/** For test usage only */
class MonotoneDummyClock(
  private var startTime: Instant =
    LocalDateTime(YEAR, JANUARY, 1, MIDDAY, 0, 0, 0).toInstant(timeZone)
) {
  fun next(): LocalDateTime {
    startTime += Duration.fromMinutes(1)
    return startTime.toLocalDateTime(timeZone)
  }
}
