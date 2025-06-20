package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.AggregateError
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.SentMessageLog
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.interfaces.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.interfaces.SentMessageLogStorage
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.util.raiseError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getError
import kotlinx.datetime.LocalDateTime

class ScheduledMessageDeliveryServiceImpl
internal constructor(
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val courseStorage: CourseStorage,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val sentMessageLogStorage: SentMessageLogStorage,
) : ScheduledMessageDeliveryService {
  override suspend fun checkAndSendMessages(
    timestamp: LocalDateTime
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val messagesToSend = scheduledMessagesDistributor.getMessagesUpToDate(timestamp).bind()
    val allErrors =
      messagesToSend.mapNotNull { scheduledMessage ->
        sendSingleMessage(scheduledMessage, timestamp).getError()
      }
    if (allErrors.isNotEmpty()) {
      raiseError(
        AggregateError("Errors while sending after messages after timestamp $timestamp", allErrors)
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
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(timestamp)
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
        .sendScheduledInformationalMessage(
          student.tgId,
          scheduledMessage.content,
          course,
          scheduledMessage.id,
        )
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
