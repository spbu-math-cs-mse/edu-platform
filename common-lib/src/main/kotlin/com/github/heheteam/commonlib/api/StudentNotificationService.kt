package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.util.DeleteMessageAction
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.flatMatrix
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StudentNotificationService(private val bot: TelegramBot) : NotificationService {
  override suspend fun notifyStudentAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
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
      if (assessment.comment.isNotEmpty()) {
        append("Комментарий преподавателя: ${assessment.comment}")
      }
    }
    with(bot) {
      val message = reply(ChatId(chatId), messageId, messageText)
      edit(
        message,
        replyMarkup =
          InlineKeyboardMarkup(
            flatMatrix {
              dataButton(
                "Ok (delete message)",
                Json.encodeToString(
                  DeleteMessageAction(message.chat.id.toChatId(), message.messageId)
                ),
              )
            }
          ),
      )
    }
  }
}
