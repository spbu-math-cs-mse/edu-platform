package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.notifications.NotificationService
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class MockNotificationService : NotificationService {
  override suspend fun notifyStudentAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SubmissionAssessment,
    problem: Problem,
  ) {
    // Do nothing
  }
}
