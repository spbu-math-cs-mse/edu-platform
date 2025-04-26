package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime

interface BotEventBus {
  fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  )

  fun publishNewSolutionEvent(solution: Solution)

  fun publishNewDeadlineRequest(studentId: StudentId, newDeadline: LocalDateTime)

  fun publishMovingDeadlineEvent(chatId: RawChatId, newDeadline: LocalDateTime)

  fun subscribeToNewSolutionEvent(handler: suspend (Solution) -> Unit)

  fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit
  )

  fun subscribeToNewDeadlineRequest(handler: suspend (StudentId, LocalDateTime) -> Unit)

  fun subscribeToMovingDeadlineEvents(handler: suspend (RawChatId, LocalDateTime) -> Unit)
}
