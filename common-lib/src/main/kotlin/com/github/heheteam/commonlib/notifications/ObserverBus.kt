package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ObserverBus(val context: CoroutineDispatcher = Dispatchers.IO) : BotEventBus {
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
      CoroutineScope(context).launch {
        it.invoke(studentId, chatId, messageId, assessment, problem)
      }
    }

  override fun publishNewSolutionEvent(solutionId: Solution) =
    newSolutionHandlers.forEach { CoroutineScope(context).launch { it.invoke(solutionId) } }

  override fun subscribeToNewSolutionEvent(handler: suspend (Solution) -> Unit) {
    newSolutionHandlers.add(handler)
  }

  override fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit
  ) {
    newGradeHandlers.add(handler)
  }
}
