package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course
import java.time.LocalDateTime

data class ScheduledMessage(
  val course: Course,
  val date: LocalDateTime,
  val message: String,
)

interface ScheduledMessagesDistributor {
  fun addMessage(message: ScheduledMessage)

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage>

  fun markMessagesUpToDateAsSent(date: LocalDateTime)
}
