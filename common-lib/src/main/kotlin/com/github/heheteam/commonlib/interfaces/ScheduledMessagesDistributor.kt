package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime

data class ScheduledMessage(
  val id: ScheduledMessageId,
  val timestamp: LocalDateTime,
  val content: TelegramMessageContent,
  val shortName: String,
  val courseId: CourseId,
  val isSent: Boolean,
  val isDeleted: Boolean,
  val adminId: AdminId,
)

interface ScheduledMessagesDistributor {
  fun sendScheduledMessage(
    adminId: AdminId,
    messageInfo: NewScheduledMessageInfo,
  ): Result<ScheduledMessageId, EduPlatformError>

  suspend fun resolveScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<ScheduledMessage, EduPlatformError>

  suspend fun viewScheduledMessages(
    adminId: AdminId?,
    courseId: CourseId?,
    lastN: Int,
  ): List<ScheduledMessage>

  suspend fun deleteScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<Unit, EduPlatformError>

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage>

  fun markMessagesUpToDateAsSent(date: LocalDateTime)
}
