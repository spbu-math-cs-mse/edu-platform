package com.github.heheteam.commonlib.testdouble

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.StateError
import com.github.heheteam.commonlib.interfaces.QuizId
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
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

class StudentBotTelegramControllerTestDouble : StudentBotTelegramController {
  private val sentMessages =
    ConcurrentHashMap<RawChatId, ConcurrentHashMap<MessageId, TelegramMessageContent>>()
  private val nextMessageId = ConcurrentHashMap<RawChatId, Long>()

  val sentQuizActivations = mutableListOf<QuizActivationCall>()
  val sentQuizEndSummaries = mutableListOf<QuizEndSummaryCall>()

  data class QuizActivationCall(
    val quizId: QuizId,
    val questionText: String,
    val answers: List<String>,
    val duration: Duration,
  )

  data class QuizEndSummaryCall(
    val quizId: QuizId,
    val chosenAnswerIndex: Int?,
    val correctAnswerIndex: Int,
    val score: Int,
  )

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

  override suspend fun notifyStudentOnGrantedAccessToChallenge(
    chatId: RawChatId,
    course: Course,
  ): Result<Unit, EduPlatformError> = Ok(Unit)

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

  override suspend fun sendQuizActivation(
    rawChatId: RawChatId,
    quizId: QuizId,
    questionText: String,
    answers: List<String>,
    duration: Duration,
  ): Result<Unit, EduPlatformError> {
    sentQuizActivations.add(QuizActivationCall(quizId, questionText, answers, duration))
    return Ok(Unit)
  }

  override suspend fun notifyOnPollQuizEnd(
    chatId: RawChatId,
    quizId: QuizId,
    chosenAnswerIndex: Int?,
    correctAnswerIndex: Int,
    score: Int,
  ): Result<Unit, EduPlatformError> {
    sentQuizEndSummaries.add(
      QuizEndSummaryCall(quizId, chosenAnswerIndex, correctAnswerIndex, score)
    )
    return Ok(Unit)
  }

  fun getSentMessages(chatId: RawChatId): Map<MessageId, TelegramMessageContent>? {
    return sentMessages[chatId]
  }

  fun clearState() {
    sentMessages.clear()
    nextMessageId.clear()
    sentQuizActivations.clear()
    sentQuizEndSummaries.clear()
  }
}
