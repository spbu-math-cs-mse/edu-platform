package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.SolutionAssessment
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

interface BotEventBus {
  fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problemId: ProblemId,
  )

  fun subscribeToGradeEvents(handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, ProblemId) -> Unit)
} 
