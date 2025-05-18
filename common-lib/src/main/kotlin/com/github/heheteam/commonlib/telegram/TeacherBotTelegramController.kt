package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

interface TeacherBotTelegramController {
  suspend fun sendInitSolutionStatusMessageDM(
    chatId: RawChatId,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  ): Result<TelegramMessageInfo, Any>

  suspend fun updateSolutionStatusMessageDM(
    message: TelegramMessageInfo,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  )

  suspend fun sendInitSolutionStatusMessageInCourseGroupChat(
    chatId: RawChatId,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  ): Result<TelegramMessageInfo, Any>

  suspend fun updateSolutionStatusMessageInCourseGroupChat(
    message: TelegramMessageInfo,
    solutionStatusMessageInfo: SolutionStatusMessageInfo,
  )

  suspend fun sendSolution(chatId: RawChatId, content: TextWithMediaAttachments)

  suspend fun sendMenuMessage(
    chatId: RawChatId,
    replyTo: TelegramMessageInfo?,
  ): Result<TelegramMessageInfo, Any>

  suspend fun deleteMessage(telegramMessageInfo: TelegramMessageInfo)
}
