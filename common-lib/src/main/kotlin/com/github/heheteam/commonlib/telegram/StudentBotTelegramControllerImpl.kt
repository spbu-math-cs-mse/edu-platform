package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

class StudentBotTelegramControllerImpl(private val studentBot: TelegramBot) :
  StudentBotTelegramController {
  override suspend fun notifyStudentOnNewAssessment(
    chatId: RawChatId,
    messageToReplyTo: MessageId,
    studentId: StudentId,
    problem: Problem,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError> {
    val emoji =
      when {
        assessment.grade <= 0 -> "❌"
        assessment.grade < problem.maxScore -> "\uD83D\uDD36"
        else -> "✅"
      }

    val messageText = buildString {
      append(
        "Ваше решение задачи ${problem.number}, серия ${problem.assignmentId} (id задачи: ${problem.id}) проверено!\n"
      )
      append("Оценка: $emoji ${assessment.grade}/${problem.maxScore}\n")
      if (assessment.comment.text != "" || assessment.comment.attachments.isNotEmpty()) {
        append("Комментарий преподавателя: ${assessment.comment.text}")
      }
    }
    return studentBot
      .sendTextWithMediaAttachments(
        chatId.toChatId(),
        assessment.comment.copy(text = messageText),
        replyTo = messageToReplyTo,
      )
      .map {}
  }

  override suspend fun notifyStudentOnDeadlineRescheduling(
    chatId: RawChatId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError> {
    return runCatching {
        studentBot.send(
          chatId.toChatId(),
          text = "Ваши дедлайны были продлены до ${newDeadline.format(deadlineFormat)}",
        )
      }
      .map {}
      .mapError { TelegramError(it) }
  }

  override suspend fun sendScheduledInformationalMessage(
    chatId: RawChatId,
    content: TelegramMessageContent,
    course: Course,
    scheduledMessageId: ScheduledMessageId,
    replyMarkup: InlineKeyboardMarkup?,
  ): Result<MessageId, EduPlatformError> {
    val messageText =
      "Сообщение от курса \"${course.name}\" (ID: ${course.id}), " +
        "ID сообщения: ${scheduledMessageId.long}\n\n${content.text}"
    val sentMessage =
      studentBot.sendTextWithMediaAttachments(
        chatId.toChatId(),
        content.copy(text = messageText),
        replyMarkup = replyMarkup,
      )
    return sentMessage.map { it.messageId }
  }

  override suspend fun deleteMessage(
    chatId: RawChatId,
    messageId: MessageId,
  ): Result<Unit, EduPlatformError> =
    runCatching {
        studentBot.deleteMessage(chatId.toChatId(), messageId)
        Unit
      }
      .mapError { TelegramError(it) }

  private val deadlineFormat =
    LocalDateTime.Format {
      date(
        LocalDate.Format {
          monthName(MonthNames.ENGLISH_ABBREVIATED)
          char(' ')
          dayOfMonth()
        }
      )
      chars(" ")
      time(
        LocalTime.Format {
          hour()
          char(':')
          minute()
        }
      )
    }
}
