package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import kotlinx.datetime.LocalDateTime

interface StudentBotTelegramController {
  suspend fun notifyStudentOnNewAssessment(
    chatId: RawChatId,
    messageToReplyTo: MessageId,
    problem: Problem,
    assignment: Assignment,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError>

  suspend fun notifyStudentOnDeadlineRescheduling(
    chatId: RawChatId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError>

  suspend fun notifyStudentOnGrantedAccessToChallenge(
    chatId: RawChatId,
    course: Course,
  ): Result<Unit, EduPlatformError>

  suspend fun sendScheduledInformationalMessage(
    chatId: RawChatId,
    scheduledMessage: ScheduledMessage,
    course: UserGroup,
    replyMarkup: InlineKeyboardMarkup? = null,
  ): Result<MessageId, EduPlatformError>

  suspend fun deleteMessage(chatId: RawChatId, messageId: MessageId): Result<Unit, EduPlatformError>

  suspend fun sendQuizActivation(
    rawChatId: RawChatId,
    quizId: QuizId,
    questionText: String,
    answers: List<String>,
    duration: kotlin.time.Duration,
  ): Result<Unit, EduPlatformError>

  suspend fun notifyOnPollQuizEnd(
    chatId: RawChatId,
    quizId: QuizId,
    chosenAnswerIndex: Int?,
    correctAnswerIndex: Int,
    score: Int,
  ): Result<Unit, EduPlatformError>
}
