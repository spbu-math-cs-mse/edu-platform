package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import kotlinx.datetime.Clock
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime

fun moscowTimeZone(): TimeZone {
  return runCatching { TimeZone.of("Europe/Moscow") }
    .mapBoth(success = { it }, failure = { FixedOffsetTimeZone(UtcOffset(hours = 3)) })
}

fun getCurrentMoscowTime(): LocalDateTime {
  return Clock.System.now().toLocalDateTime(moscowTimeZone())
}

fun getCurrentInstant(): Instant {
  return Clock.System.now()
}
