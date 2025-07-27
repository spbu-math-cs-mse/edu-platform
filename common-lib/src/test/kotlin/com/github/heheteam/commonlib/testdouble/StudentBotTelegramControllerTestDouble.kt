package com.github.heheteam.commonlib.testdouble

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.StateError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import java.util.concurrent.ConcurrentHashMap
import kotlinx.datetime.LocalDateTime

class StudentBotTelegramControllerTestDouble : StudentBotTelegramController {
  private val sentMessages =
    ConcurrentHashMap<RawChatId, ConcurrentHashMap<MessageId, TelegramMessageContent>>()
  private val nextMessageId = ConcurrentHashMap<RawChatId, Long>()

  override suspend fun notifyStudentOnNewAssessment(
    chatId: RawChatId,
    messageToReplyTo: MessageId,
    studentId: StudentId,
    problem: Problem,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError> = Ok(Unit)

  override suspend fun notifyStudentOnDeadlineRescheduling(
    chatId: RawChatId,
    newDeadline: LocalDateTime,
  ) = Ok(Unit)

  override suspend fun sendScheduledInformationalMessage(
    chatId: RawChatId,
    scheduledMessage: ScheduledMessage,
    course: UserGroup,
    replyMarkup: InlineKeyboardMarkup?,
  ): Result<MessageId, EduPlatformError> {
    val currentId = nextMessageId.compute(chatId) { _, oldId -> (oldId ?: 0L) + 1 }!!
    val newMessageId = MessageId(currentId)
    sentMessages.computeIfAbsent(chatId) { ConcurrentHashMap() }[newMessageId] =
      scheduledMessage.content
    return Ok(newMessageId)
  }

  override suspend fun deleteMessage(
    chatId: RawChatId,
    messageId: MessageId,
  ): Result<Unit, EduPlatformError> {
    val chatMessages = sentMessages[chatId]
    return if (chatMessages?.remove(messageId) != null) {
      Ok(Unit)
    } else {
      Err(
        StateError(
          "Message with ID ${messageId.long} not found for chat ID ${chatId.long}",
          this::class,
        )
      )
    }
  }

  // Helper for tests to inspect sent messages
  fun getSentMessages(chatId: RawChatId): Map<MessageId, TelegramMessageContent>? {
    return sentMessages[chatId]
  }

  // Helper for tests to reset state
  fun clearState() {
    sentMessages.clear()
    nextMessageId.clear()
  }
}
