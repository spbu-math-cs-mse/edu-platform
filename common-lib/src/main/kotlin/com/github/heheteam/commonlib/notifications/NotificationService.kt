package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

interface NotificationService {
  suspend fun notifyStudentAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  )
}
