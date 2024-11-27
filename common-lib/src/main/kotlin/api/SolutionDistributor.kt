package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime

interface SolutionDistributor {
  fun inputSolution(
    studentId: Long,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  )

  fun querySolution(teacherId: Long): Solution?

  fun assessSolution(
    solution: Solution,
    teacherId: Long,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: LocalDateTime = LocalDateTime.now(),
    teacherStatistics: TeacherStatistics,
  )
}
