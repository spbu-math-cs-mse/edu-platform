package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.SentMessageLog
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.database.DatabaseScheduledMessagesStorage
import com.github.heheteam.commonlib.errors.AggregateError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.SentMessageLogStorage
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.util.raiseError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.onFailure
import kotlinx.datetime.LocalDateTime

class ScheduledMessageService
internal constructor(
  private val scheduledMessagesStorage: DatabaseScheduledMessagesStorage,
  private val sentMessageLogStorage: SentMessageLogStorage,
  private val courseStorage: CourseStorage,
  private val studentBotTelegramController: StudentBotTelegramController,
) {
  fun sendScheduledMessage(
    adminId: AdminId,
    messageInfo: NewScheduledMessageInfo,
  ): Result<ScheduledMessageId, EduPlatformError> =
    scheduledMessagesStorage.storeScheduledMessage(adminId, messageInfo)

  fun resolveScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<ScheduledMessage, EduPlatformError> =
    scheduledMessagesStorage.resolveScheduledMessage(scheduledMessageId)

  fun viewScheduledMessages(
    adminId: AdminId?,
    courseId: CourseId?,
    lastN: Int,
  ): Result<List<ScheduledMessage>, EduPlatformError> =
    scheduledMessagesStorage.viewScheduledMessages(adminId, courseId, lastN)

  suspend fun deleteScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val updatedRows = scheduledMessagesStorage.setIsDeleted(scheduledMessageId).bind()
    if (updatedRows == 0) {
      Err(ResolveError(scheduledMessageId, "ScheduledMessage")).bind()
    } else {
      val sentLogs = sentMessageLogStorage.getSentMessageLogs(scheduledMessageId).bind()
      sentLogs.forEach { log ->
        studentBotTelegramController.deleteMessage(log.chatId, log.telegramMessageId).onFailure {
          error ->
          println(
            "Failed to delete Telegram message for scheduled message " +
              "${scheduledMessageId.long}, log ${log.logId}: ${error.shortDescription}"
          )
        }
      }
    }
  }

  suspend fun checkAndSendMessages(timestamp: LocalDateTime): Result<Unit, EduPlatformError> =
    coroutineBinding {
      val messagesToSend = scheduledMessagesStorage.getMessagesUpToDate(timestamp).bind()
      val allErrors =
        messagesToSend.mapNotNull { scheduledMessage ->
          sendSingleMessage(scheduledMessage, timestamp).getError()
        }
      if (allErrors.isNotEmpty()) {
        raiseError(
          AggregateError(
            "Errors while sending after messages after timestamp $timestamp",
            allErrors,
          )
        )
      }
      Unit
    }

  private suspend fun sendSingleMessage(
    scheduledMessage: ScheduledMessage,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val course = courseStorage.resolveCourse(scheduledMessage.courseId).bind()

    val studentsInCourse = courseStorage.getStudents(scheduledMessage.courseId).bind()

    val errors =
      studentsInCourse.mapNotNull { student ->
        sendMessageToStudentTg(student, scheduledMessage, course, timestamp).getError()
      }
    scheduledMessagesStorage.markMessagesUpToDateAsSent(timestamp)
    if (errors.isNotEmpty()) {
      Err(
          AggregateError("Errors while sending scheduled message id=${scheduledMessage.id}", errors)
        )
        .bind()
    }
  }

  private suspend fun sendMessageToStudentTg(
    student: Student,
    scheduledMessage: ScheduledMessage,
    course: Course,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val sentMessage =
      studentBotTelegramController
        .sendScheduledInformationalMessage(student.tgId, scheduledMessage, course)
        .bind()
    sentMessageLogStorage
      .logSentMessage(
        SentMessageLog(
          logId = 0,
          scheduledMessageId = scheduledMessage.id,
          studentId = student.id,
          sentTimestamp = timestamp,
          telegramMessageId = sentMessage,
          chatId = student.tgId,
        )
      )
      .bind()
  }
}
