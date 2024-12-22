package com.github.heheteam.commonlib.api

import java.time.LocalDateTime

data class ScheduledMessage(
  val courseId: CourseId,
  val date: LocalDateTime,
  val message: String,
)

interface ScheduledMessagesDistributor {
  fun addMessage(message: ScheduledMessage)

  fun getUnsentMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage>

  fun markMessagesUpToDateAsSent(date: LocalDateTime)
}
