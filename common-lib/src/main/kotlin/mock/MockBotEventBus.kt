package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.StudentId
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

  override fun subscribeToGradeEvents(handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit) {
    // Do nothing
  }
} 
