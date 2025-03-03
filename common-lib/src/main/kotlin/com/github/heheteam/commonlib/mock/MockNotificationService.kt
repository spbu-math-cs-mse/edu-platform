package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.NotificationService
import com.github.heheteam.commonlib.api.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class MockNotificationService : NotificationService {
  override suspend fun notifyStudentAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    // Do nothing
  }
}
