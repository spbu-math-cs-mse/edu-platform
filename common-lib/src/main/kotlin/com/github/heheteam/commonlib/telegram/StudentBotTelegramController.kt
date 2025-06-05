package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import kotlinx.datetime.LocalDateTime

interface StudentBotTelegramController {
  suspend fun notifyStudentOnNewAssessment(
    chatId: RawChatId,
    messageToReplyTo: MessageId,
    studentId: StudentId,
    problem: Problem,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError>

  suspend fun notifyStudentOnDeadlineRescheduling(
    chatId: RawChatId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError>

  suspend fun sendScheduledInformationalMessage(
    chatId: RawChatId,
    content: TelegramMessageContent,
    course: Course,
    scheduledMessageId: ScheduledMessageId,
    replyMarkup: InlineKeyboardMarkup? = null,
  ): Result<MessageId, EduPlatformError>

  suspend fun deleteMessage(chatId: RawChatId, messageId: MessageId): Result<Unit, EduPlatformError>
}
