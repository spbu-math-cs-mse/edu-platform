package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class StudentNotificationService(
  private val bot: TelegramBot,
) : NotificationService {
  override suspend fun notifyStudentAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    val emoji = when {
      assessment.grade <= 0 -> "❌"
      assessment.grade < problem.maxScore -> "\uD83D\uDD36"
      else -> "✅"
    }

    val message = buildString {
      append("Ваше решение задачи ${problem.number}, серия ${problem.assignmentId.id} (id задачи: ${problem.id}) проверено!\n")
      append("Оценка: $emoji ${assessment.grade}/${problem.maxScore}\n")
      if (assessment.comment.isNotEmpty()) {
        append("Комментарий преподавателя: ${assessment.comment}")
      }
    }

    bot.reply(
      ChatId(chatId),
      messageId,
      message,
    )
  }
}
