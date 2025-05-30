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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getError
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime

class ScheduledMessageDeliveryServiceImpl
internal constructor(
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val courseStorage: CourseStorage,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val sentMessageLogStorage: SentMessageLogStorage,
) : ScheduledMessageDeliveryService {
  override fun checkAndSendMessages(timestamp: LocalDateTime): Result<Unit, EduPlatformError> {
    val messagesToSend = scheduledMessagesDistributor.getMessagesUpToDate(timestamp)
    val allErrors =
      messagesToSend.mapNotNull { scheduledMessage ->
        sendSingleMessage(scheduledMessage, timestamp).getError()
      }
    return if (allErrors.isNotEmpty()) {
      Err(
        AggregateError("Errors while sending after messages after timestamp $timestamp", allErrors)
      )
    } else {
      Ok(Unit)
    }
  }

  private fun sendSingleMessage(
    scheduledMessage: ScheduledMessage,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> = binding {
    val course = courseStorage.resolveCourse(scheduledMessage.courseId).bind()

    val studentsInCourse = courseStorage.getStudents(scheduledMessage.courseId)

    val errors =
      studentsInCourse.mapNotNull { student ->
        sendMessageToStudentTg(student, scheduledMessage, course, timestamp).getError()
      }
    if (errors.isNotEmpty()) {
      Err(
          AggregateError("Errors while sending scheduled message id=${scheduledMessage.id}", errors)
        )
        .bind()
    }
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(timestamp)
  }

  private fun sendMessageToStudentTg(
    student: Student,
    scheduledMessage: ScheduledMessage,
    course: Course,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> = binding {
    val sentMessage =
      runBlocking {
          studentBotTelegramController.sendScheduledInformationalMessage(
            student.tgId,
            scheduledMessage.content,
            course,
            scheduledMessage.id,
          )
        }
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
