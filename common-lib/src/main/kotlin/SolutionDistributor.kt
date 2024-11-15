package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

interface SolutionDistributor {
  fun inputSolution(
    studentId: String,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  ): Solution

  fun querySolution(teacherId: String): Solution?

  fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
  )
}
