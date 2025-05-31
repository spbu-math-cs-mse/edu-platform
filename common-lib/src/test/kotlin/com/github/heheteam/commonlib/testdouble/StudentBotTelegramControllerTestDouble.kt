package com.github.heheteam.commonlib.testdouble

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.NamedError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.StudentId
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
  ) {
    // Not relevant for this test, do nothing
  }

  override suspend fun notifyStudentOnDeadlineRescheduling(
    chatId: RawChatId,
    newDeadline: LocalDateTime,
  ) {
    // Not relevant for this test, do nothing
  }

  override suspend fun sendScheduledInformationalMessage(
    chatId: RawChatId,
    content: TelegramMessageContent,
    course: Course,
    scheduledMessageId: ScheduledMessageId,
    replyMarkup: InlineKeyboardMarkup?,
  ): Result<MessageId, EduPlatformError> {
    val currentId = nextMessageId.compute(chatId) { _, oldId -> (oldId ?: 0L) + 1 }!!
    val newMessageId = MessageId(currentId)
    sentMessages.computeIfAbsent(chatId) { ConcurrentHashMap() }[newMessageId] = content
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
      Err(NamedError("Message with ID ${messageId.long} not found for chat ID ${chatId.long}"))
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
