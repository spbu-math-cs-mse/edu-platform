package com.github.heheteam.studentbot.logic

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.StudentId
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class StudentNotificationService : NotificationService, TelegramBotController {
  private var lateInitStudentBot: TelegramBot? = null

  override suspend fun notifyStudentAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    runBlocking(Dispatchers.IO) {
      coroutineBinding {
        val emoji =
          when {
            assessment.grade <= 0 -> "❌"
            assessment.grade < problem.maxScore -> "\uD83D\uDD36"
            else -> "✅"
          }

        val message = buildString {
          append(
            "Ваше решение задачи ${problem.number}, серия ${problem.assignmentId}" +
              " (id задачи: ${problem.id}) проверено!\n"
          )
          append("Оценка: $emoji ${assessment.grade}/${problem.maxScore}\n")
          if (assessment.comment.isNotEmpty()) {
            append("Комментарий преподавателя: ${assessment.comment}")
          }
        }

        val bot = lateInitStudentBot.toResultOr { "uninitialized telegram bot" }.bind()
        bot.reply(ChatId(chatId), messageId, message)
      }
    }
  }

  override fun setTelegramBot(telegramBot: TelegramBot) {
    lateInitStudentBot = telegramBot
  }
}
