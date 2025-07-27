package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.SentMessageLog
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.database.DatabaseScheduledMessagesStorage
import com.github.heheteam.commonlib.errors.AggregateError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.EduPlatformResult
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.SentMessageLogStorage
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.repository.CourseRepository
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.util.raiseError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.onFailure
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ScheduledMessageService
internal constructor(
  private val scheduledMessagesStorage: DatabaseScheduledMessagesStorage,
  private val sentMessageLogStorage: SentMessageLogStorage,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val courseRepository: CourseRepository,
  private val studentStorage: StudentStorage,
  private val database: Database,
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
    newSuspendedTransaction(db = database) {
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
    }

  private suspend fun sendSingleMessage(
    scheduledMessage: ScheduledMessage,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val students = resolveByUserGroup(scheduledMessage.userGroup).bind()

    val errors =
      students.mapNotNull { student ->
        sendMessageToStudentTg(student, scheduledMessage, scheduledMessage.userGroup, timestamp)
          .getError()
      }
    scheduledMessagesStorage.markMessagesUpToDateAsSent(timestamp)
    if (errors.isNotEmpty()) {
      Err(
          AggregateError("Errors while sending scheduled message id=${scheduledMessage.id}", errors)
        )
        .bind()
    }
  }

  private fun resolveByUserGroup(userGroup: UserGroup): EduPlatformResult<List<Student>> {
    return when (userGroup) {
      is UserGroup.AllRegisteredUsers -> studentStorage.getAll()
      is UserGroup.CompletedQuest -> studentStorage.getWithCompletedQuest()
      is UserGroup.CourseGroup ->
        return binding {
          val course = courseRepository.findById(userGroup.courseId).bind()
          val studentsInCourse =
            course.students.mapNotNull { studentStorage.resolveStudent(it).bind() }
          studentsInCourse
        }
    }
  }

  private suspend fun sendMessageToStudentTg(
    student: Student,
    scheduledMessage: ScheduledMessage,
    userGroup: UserGroup,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val sentMessage =
      studentBotTelegramController
        .sendScheduledInformationalMessage(student.tgId, scheduledMessage, userGroup)
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
