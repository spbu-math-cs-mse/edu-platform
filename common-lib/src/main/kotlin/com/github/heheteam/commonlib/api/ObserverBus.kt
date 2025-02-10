package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ObserverBus : BotEventBus {
  private val newSolutionHandlers = mutableListOf<suspend (Solution) -> Unit>()
  private val newGradeHandlers =
    mutableListOf<suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit>()

  override fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) =
    newGradeHandlers.forEach {
      CoroutineScope(Dispatchers.IO).launch {
        it.invoke(studentId, chatId, messageId, assessment, problem)
      }
    }

  override fun publishNewSolutionEvent(solutionId: Solution) =
    newSolutionHandlers.forEach { CoroutineScope(Dispatchers.IO).launch { it.invoke(solutionId) } }

  override fun subscribeToNewSolutionEvent(handler: suspend (Solution) -> Unit) {
    newSolutionHandlers.add(handler)
  }

  override fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit
  ) {
    newGradeHandlers.add(handler)
  }
}
