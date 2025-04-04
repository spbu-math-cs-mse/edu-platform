package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.notifications.BotEventBus
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class MockBotEventBus : BotEventBus {
  override fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    // Do nothing
  }

  override fun publishNewSolutionEvent(solutionId: Solution) {
    // Do nothing
  }

  override fun subscribeToNewSolutionEvent(handler: suspend (Solution) -> Unit) {
    // Do nothing
  }

  override fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit
  ) {
    // Do nothing
  }
}
