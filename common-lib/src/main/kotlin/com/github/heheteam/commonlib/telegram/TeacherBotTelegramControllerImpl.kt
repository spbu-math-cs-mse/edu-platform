package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.logic.ui.createSolutionGradingKeyboard
import com.github.heheteam.commonlib.util.delete
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId

class TeacherBotTelegramControllerImpl(private val teacherBot: TelegramBot) :
  TeacherBotTelegramController {
  override suspend fun sendInitSolutionStatusMessageDM(
    chatId: RawChatId,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  ) {
    TODO("Not yet implemented")
  }

  override suspend fun updateSolutionStatusMessageDM(
    message: TelegramMessageInfo,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  ) {
    val messageContent = solutionStatusInfoToMessageContent(solutionStatusMessageInfo)
    teacherBot.edit(message.chatId.toChatId(), message.messageId, messageContent)

    teacherBot.editMessageReplyMarkup(
      message.chatId.toChatId(),
      message.messageId,
      replyMarkup = createSolutionGradingKeyboard(solutionStatusMessageInfo.solutionId),
    )
  }

  override suspend fun sendInitSolutionStatusMessageInCourseGroupChat(
    chatId: RawChatId,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  ) {
    TODO("Not yet implemented")
  }

  override suspend fun updateSolutionStatusMessageInCourseGroupChat(
    message: TelegramMessageInfo,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  ) {
    val messageContent = solutionStatusInfoToMessageContent(solutionStatusMessageInfo)
    teacherBot.edit(message.chatId.toChatId(), message.messageId, messageContent)

    teacherBot.editMessageReplyMarkup(
      message.chatId.toChatId(),
      message.messageId,
      replyMarkup = createSolutionGradingKeyboard(solutionStatusMessageInfo.solutionId),
    )
  }

  override suspend fun sendMenuMessage(replyTo: TelegramMessageInfo?) {
    TODO("Not yet implemented")
  }

  override suspend fun deleteMessage(telegramMessageInfo: TelegramMessageInfo) {
    teacherBot.delete(telegramMessageInfo.chatId.toChatId(), telegramMessageInfo.messageId)
  }

  private fun solutionStatusInfoToMessageContent(
    solutionStatusMessageInfo: SolutionStatusMessageInfo
  ): String {
    val student = solutionStatusMessageInfo.student
    val responsibleTeacher = solutionStatusMessageInfo.responsibleTeacher
    val gradingEntries = solutionStatusMessageInfo.gradingEntries
    return buildString {
      appendLine("(Ответьте на это сообщение или нажмите на кнопки внизу для проверки)")
      appendLine("Отправка #${solutionStatusMessageInfo.solutionId.long}")
      appendLine(
        "Задача ${solutionStatusMessageInfo.assignmentDisplayName}:${solutionStatusMessageInfo.problemDisplayName}"
      )
      appendLine("Решение отправил ${student.name} ${student.surname} (id=${student.id})")
      if (responsibleTeacher != null)
        appendLine(
          "Проверяющий: ${responsibleTeacher.name} ${responsibleTeacher.surname} (id=${responsibleTeacher.id})"
        )
      appendLine()

      gradingEntries.forEach { entry: GradingEntry ->
        appendLine("Проверил учитель id=${entry.teacherId.long}}")
        appendLine("Дата: ${entry.timestamp}")
        appendLine("Оценка: ${entry.assessment.grade}")
        appendLine("Комментарий: \"${entry.assessment.comment.text}\"")
        appendLine("---")
      }
    }
  }
}
