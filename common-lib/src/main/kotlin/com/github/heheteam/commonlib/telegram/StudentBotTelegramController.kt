package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime

interface StudentBotTelegramController {
  suspend fun notifyStudentOnNewAssessment(
    chatId: RawChatId,
    messageToReplyTo: MessageId,
    studentId: StudentId,
    problem: Problem,
    assessment: SolutionAssessment,
  )

  suspend fun notifyStudentOnDeadlineRescheduling(chatId: RawChatId, newDeadline: LocalDateTime)
}
