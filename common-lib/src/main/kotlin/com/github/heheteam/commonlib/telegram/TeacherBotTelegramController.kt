package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

interface TeacherBotTelegramController {
  suspend fun sendInitSubmissionStatusMessageDM(
    chatId: RawChatId,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<TelegramMessageInfo, EduPlatformError>

  suspend fun updateSubmissionStatusMessageDM(
    message: TelegramMessageInfo,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  )

  suspend fun sendInitSubmissionStatusMessageInCourseGroupChat(
    chatId: RawChatId,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<TelegramMessageInfo, EduPlatformError>

  suspend fun updateSubmissionStatusMessageInCourseGroupChat(
    message: TelegramMessageInfo,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  )

  suspend fun sendSubmission(chatId: RawChatId, content: TextWithMediaAttachments)

  suspend fun sendMenuMessage(
    chatId: RawChatId,
    replyTo: TelegramMessageInfo?,
  ): Result<TelegramMessageInfo, EduPlatformError>

  suspend fun deleteMessage(telegramMessageInfo: TelegramMessageInfo)
}
