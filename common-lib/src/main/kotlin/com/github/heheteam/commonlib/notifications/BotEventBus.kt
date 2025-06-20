package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime

interface BotEventBus {
  fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SubmissionAssessment,
    problem: Problem,
  )

  suspend fun publishNewSubmissionEvent(submission: Submission)

  suspend fun publishNewDeadlineRequest(studentId: StudentId, newDeadline: LocalDateTime)

  suspend fun publishMovingDeadlineEvent(chatId: RawChatId, newDeadline: LocalDateTime)

  fun subscribeToNewSubmissionEvent(handler: suspend (Submission) -> Unit)

  fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SubmissionAssessment, Problem) -> Unit
  )

  fun subscribeToNewDeadlineRequest(handler: suspend (StudentId, LocalDateTime) -> Unit)

  fun subscribeToMovingDeadlineEvents(handler: suspend (RawChatId, LocalDateTime) -> Unit)
}
