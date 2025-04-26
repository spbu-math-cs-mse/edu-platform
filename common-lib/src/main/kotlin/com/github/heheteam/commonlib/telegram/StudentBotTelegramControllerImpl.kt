package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
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
    assessment: SolutionAssessment,
  ) {
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
    studentBot.sendTextWithMediaAttachments(
      chatId.toChatId(),
      assessment.comment.copy(text = messageText),
      replyTo = messageToReplyTo,
    )
  }

  override suspend fun notifyStudentOnDeadlineRescheduling(
    chatId: RawChatId,
    newDeadline: LocalDateTime,
  ) {
    studentBot.send(
      chatId.toChatId(),
      text = "Ваши дедлайны были продлены до ${newDeadline.format(deadlineFormat)}",
    )
  }

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
