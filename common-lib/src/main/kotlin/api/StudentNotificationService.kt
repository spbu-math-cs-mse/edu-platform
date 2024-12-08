package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.SolutionAssessment
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class StudentNotificationService(
    private val bot: TelegramBot
) : NotificationService {
    override suspend fun notifyStudentAboutGrade(
      studentId: StudentId,
      chatId: RawChatId,
      messageId: MessageId,
      assessment: SolutionAssessment,
      problemId: ProblemId
    ) {
        println("Preparing notification for student $studentId")
        val emoji = when {
            assessment.grade <= 0 -> "❌"
            assessment.grade < 5 -> "\uD83D\uDD36"
            else -> "✅"
        }
        
        val message = buildString {
            append("Ваше решение задачи $problemId проверено!\n")
            append("Оценка: $emoji ${assessment.grade}/5\n")
            if (assessment.comment.isNotEmpty()) {
                append("Комментарий преподавателя: ${assessment.comment}")
            }
        }

        println("Sending notification to chat $chatId")
        bot.reply(
            ChatId(chatId),
                messageId,
                message
        )
        println("Notification sent successfully")
    }
}