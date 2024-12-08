package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.SolutionAssessment
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

interface NotificationService {
    suspend fun notifyStudentAboutGrade(
      studentId: StudentId,
      chatId: RawChatId,
      messageId: MessageId,
      assessment: SolutionAssessment,
      problemId: ProblemId
    )
} 