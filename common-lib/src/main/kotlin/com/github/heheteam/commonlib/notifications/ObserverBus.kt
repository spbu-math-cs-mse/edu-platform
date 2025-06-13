package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class ObserverBus(val context: CoroutineDispatcher = Dispatchers.IO) : BotEventBus {
  private val newSubmissionHandlers = mutableListOf<suspend (Submission) -> Unit>()
  private val newGradeHandlers =
    mutableListOf<
      suspend (StudentId, RawChatId, MessageId, SubmissionAssessment, Problem) -> Unit
    >()

  private val newDeadlineRequestHandlers =
    mutableListOf<suspend (StudentId, LocalDateTime) -> Unit>()
  private val movingDeadlineHandlers = mutableListOf<suspend (RawChatId, LocalDateTime) -> Unit>()

  override fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SubmissionAssessment,
    problem: Problem,
  ) =
    newGradeHandlers.forEach {
      CoroutineScope(context).launch {
        it.invoke(studentId, chatId, messageId, assessment, problem)
      }
    }

  override suspend fun publishNewSubmissionEvent(submission: Submission) =
    newSubmissionHandlers.forEach { it.invoke(submission) }

  override suspend fun publishNewDeadlineRequest(studentId: StudentId, newDeadline: LocalDateTime) {
    newDeadlineRequestHandlers.forEach { it.invoke(studentId, newDeadline) }
  }

  override suspend fun publishMovingDeadlineEvent(chatId: RawChatId, newDeadline: LocalDateTime) {
    movingDeadlineHandlers.forEach { it.invoke(chatId, newDeadline) }
  }

  override fun subscribeToNewSubmissionEvent(handler: suspend (Submission) -> Unit) {
    newSubmissionHandlers.add(handler)
  }

  override fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SubmissionAssessment, Problem) -> Unit
  ) {
    newGradeHandlers.add(handler)
  }

  override fun subscribeToNewDeadlineRequest(handler: suspend (StudentId, LocalDateTime) -> Unit) {
    newDeadlineRequestHandlers.add(handler)
  }

  override fun subscribeToMovingDeadlineEvents(
    handler: suspend (RawChatId, LocalDateTime) -> Unit
  ) {
    movingDeadlineHandlers.add(handler)
  }
}
