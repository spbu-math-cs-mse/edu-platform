package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime

class ObserverBus(val context: CoroutineDispatcher = Dispatchers.IO) : BotEventBus {
  private val newSolutionHandlers = mutableListOf<suspend (Solution) -> Unit>()
  private val newGradeHandlers =
    mutableListOf<suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit>()

  private val newDeadlineRequestHandlers =
    mutableListOf<suspend (StudentId, LocalDateTime) -> Unit>()
  private val movingDeadlineHandlers = mutableListOf<suspend (RawChatId, LocalDateTime) -> Unit>()

  override fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) =
    newGradeHandlers.forEach {
      CoroutineScope(context).launch {
        it.invoke(studentId, chatId, messageId, assessment, problem)
      }
    }

  override fun publishNewSolutionEvent(solution: Solution) =
    newSolutionHandlers.forEach { runBlocking { it.invoke(solution) } }

  override fun publishNewDeadlineRequest(studentId: StudentId, newDeadline: LocalDateTime) {
    newDeadlineRequestHandlers.forEach { runBlocking { it.invoke(studentId, newDeadline) } }
  }

  override fun publishMovingDeadlineEvent(chatId: RawChatId, newDeadline: LocalDateTime) {
    movingDeadlineHandlers.forEach { runBlocking { it.invoke(chatId, newDeadline) } }
  }

  override fun subscribeToNewSolutionEvent(handler: suspend (Solution) -> Unit) {
    newSolutionHandlers.add(handler)
  }

  override fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit
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
