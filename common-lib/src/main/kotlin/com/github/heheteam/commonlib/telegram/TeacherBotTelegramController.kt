package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.errors.EduPlatformError
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
  ): Result<Unit, EduPlatformError>

  suspend fun sendInitSubmissionStatusMessageInCourseGroupChat(
    chatId: RawChatId,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<TelegramMessageInfo, EduPlatformError>

  suspend fun updateSubmissionStatusMessageInCourseGroupChat(
    message: TelegramMessageInfo,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<Unit, EduPlatformError>

  suspend fun sendSubmission(
    chatId: RawChatId,
    content: TextWithMediaAttachments,
  ): Result<Unit, EduPlatformError>

  suspend fun sendMenuMessage(
    chatId: RawChatId,
    replyTo: TelegramMessageInfo?,
  ): Result<TelegramMessageInfo, EduPlatformError>

  suspend fun deleteMessage(
    telegramMessageInfo: TelegramMessageInfo
  ): Result<Unit, EduPlatformError>

  @Suppress("LongParameterList") // all the data is needed here
  suspend fun sendQuizOverallResult(
    chatId: RawChatId,
    questionText: String,
    totalParticipants: Int,
    correctAnswers: Int,
    incorrectAnswers: Int,
    notAnswered: Int,
  ): Result<Unit, EduPlatformError>
}
