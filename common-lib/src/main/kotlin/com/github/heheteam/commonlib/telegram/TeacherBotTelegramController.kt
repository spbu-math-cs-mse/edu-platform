package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.TelegramMessageInfo
import dev.inmo.tgbotapi.types.RawChatId

interface TeacherBotTelegramController {
  suspend fun sendInitSolutionStatusMessageDM(
    chatId: RawChatId,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  )

  suspend fun updateSolutionStatusMessageDM(
    message: TelegramMessageInfo,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  )

  suspend fun sendInitSolutionStatusMessageInCourseGroupChat(
    chatId: RawChatId,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  )

  suspend fun updateSolutionStatusMessageInCourseGroupChat(
    message: TelegramMessageInfo,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  )

  suspend fun sendMenuMessage(replyTo: TelegramMessageInfo?)

  suspend fun deleteMessage(telegramMessageInfo: TelegramMessageInfo)
}
