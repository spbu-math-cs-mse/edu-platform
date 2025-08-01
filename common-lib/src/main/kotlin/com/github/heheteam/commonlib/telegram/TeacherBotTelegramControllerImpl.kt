package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.TelegramError
import com.github.heheteam.commonlib.errors.asNamedError
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.logic.ui.createSubmissionGradingKeyboard
import com.github.heheteam.commonlib.toTelegramMessageInfo
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regularln

class TeacherBotTelegramControllerImpl(private val teacherBot: TelegramBot) :
  TeacherBotTelegramController {
  override suspend fun sendInitSubmissionStatusMessageDM(
    chatId: RawChatId,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<TelegramMessageInfo, EduPlatformError> =
    runCatching {
        val messageContent = submissionStatusInfoToMessageContent(submissionStatusMessageInfo)
        val message = teacherBot.sendMessage(chatId.toChatId(), messageContent)
        teacherBot.editMessageReplyMarkup(
          message.chat,
          message.messageId,
          replyMarkup = createSubmissionGradingKeyboard(submissionStatusMessageInfo.submissionId),
        )
        message.toTelegramMessageInfo()
      }
      .mapError { TelegramError(it) }

  override suspend fun updateSubmissionStatusMessageDM(
    message: TelegramMessageInfo,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<Unit, EduPlatformError> =
    runCatching {
        val messageContent = submissionStatusInfoToMessageContent(submissionStatusMessageInfo)
        teacherBot.edit(message.chatId.toChatId(), message.messageId, messageContent)

        teacherBot.editMessageReplyMarkup(
          message.chatId.toChatId(),
          message.messageId,
          replyMarkup = createSubmissionGradingKeyboard(submissionStatusMessageInfo.submissionId),
        )
      }
      .mapError { TelegramError(it) }
      .map { Unit }

  override suspend fun sendInitSubmissionStatusMessageInCourseGroupChat(
    chatId: RawChatId,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<TelegramMessageInfo, EduPlatformError> =
    runCatching {
        val messageContent = submissionStatusInfoToMessageContent(submissionStatusMessageInfo)
        val message = teacherBot.sendMessage(chatId.toChatId(), messageContent)
        teacherBot.editMessageReplyMarkup(
          message.chat,
          message.messageId,
          replyMarkup = createSubmissionGradingKeyboard(submissionStatusMessageInfo.submissionId),
        )
        message.toTelegramMessageInfo()
      }
      .mapError { TelegramError(it) }

  override suspend fun updateSubmissionStatusMessageInCourseGroupChat(
    message: TelegramMessageInfo,
    submissionStatusMessageInfo: SubmissionStatusMessageInfo,
  ): Result<Unit, EduPlatformError> =
    runCatching {
        val messageContent = submissionStatusInfoToMessageContent(submissionStatusMessageInfo)
        teacherBot.edit(message.chatId.toChatId(), message.messageId, messageContent)

        teacherBot.editMessageReplyMarkup(
          message.chatId.toChatId(),
          message.messageId,
          replyMarkup = createSubmissionGradingKeyboard(submissionStatusMessageInfo.submissionId),
        )
      }
      .mapError { TelegramError(it) }
      .map { Unit }

  override suspend fun sendSubmission(
    chatId: RawChatId,
    content: TextWithMediaAttachments,
  ): Result<Unit, EduPlatformError> =
    teacherBot.sendTextWithMediaAttachments(chatId.toChatId(), content).map { Unit }

  override suspend fun sendMenuMessage(
    chatId: RawChatId,
    replyTo: TelegramMessageInfo?,
  ): Result<TelegramMessageInfo, EduPlatformError> =
    runCatching {
        if (replyTo != null) {
            teacherBot.reply(replyTo.chatId.toChatId(), replyTo.messageId, "\u2705 Главное меню")
          } else {
            teacherBot.sendMessage(chatId.toChatId(), "\u2705 Главное меню")
          }
          .toTelegramMessageInfo()
      }
      .mapError { "".asNamedError(TeacherBotTelegramControllerImpl::class) }

  override suspend fun deleteMessage(
    telegramMessageInfo: TelegramMessageInfo
  ): Result<Unit, EduPlatformError> =
    runCatching {
        teacherBot.delete(telegramMessageInfo.chatId.toChatId(), telegramMessageInfo.messageId)
      }
      .mapError {
        KSLog.warning("Failed to delete message", it)
        TelegramError(it)
      }
      .map { Unit }

  private fun submissionStatusInfoToMessageContent(
    submissionStatusMessageInfo: SubmissionStatusMessageInfo
  ): TextSourcesList {
    val student = submissionStatusMessageInfo.student
    val responsibleTeacher = submissionStatusMessageInfo.responsibleTeacher
    val gradingEntries = submissionStatusMessageInfo.gradingEntries
    return buildEntities {
      regularln("(Ответьте на это сообщение или нажмите на кнопки внизу для проверки)")
      regularln("Отправка #${submissionStatusMessageInfo.submissionId.long}")
      regularln(
        "Задача ${submissionStatusMessageInfo.assignmentDisplayName}:${submissionStatusMessageInfo.problemDisplayName}"
      )
      regularln("Решение отправил ${student.name} ${student.surname} (id=${student.id})")
      if (responsibleTeacher != null)
        regularln(
          "Проверяющий: ${responsibleTeacher.name} ${responsibleTeacher.surname} (id=${responsibleTeacher.id})"
        )
      regularln("")

      gradingEntries.forEach { entry: GradingEntry ->
        regularln("Проверил учитель id=${entry.teacherId.long}")
        regularln("Дата: ${entry.timestamp}")
        regularln("Оценка: ${entry.assessment.grade}")
        regularln("Комментарий: ")
        addAll(entry.assessment.comment.text)
        regularln("\n---\n")
      }
    }
  }
}
