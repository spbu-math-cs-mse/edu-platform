package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.MaybeEduPlatformError
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
  fun storeScheduledMessage(
    adminId: AdminId,
    messageInfo: NewScheduledMessageInfo,
  ): Result<ScheduledMessageId, EduPlatformError>

  fun resolveScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<ScheduledMessage, EduPlatformError>

  fun viewScheduledMessages(
    adminId: AdminId?,
    courseId: CourseId?,
    lastN: Int,
  ): Result<List<ScheduledMessage>, EduPlatformError>

  suspend fun deleteScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<Unit, EduPlatformError>

  fun getMessagesUpToDate(date: LocalDateTime): Result<List<ScheduledMessage>, EduPlatformError>

  fun markMessagesUpToDateAsSent(date: LocalDateTime): MaybeEduPlatformError
}
