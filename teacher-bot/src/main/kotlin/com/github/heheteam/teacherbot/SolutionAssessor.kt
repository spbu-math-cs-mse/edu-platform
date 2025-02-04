package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.michaelbull.result.map
import java.time.LocalDateTime

class SolutionAssessor(
  private val teacherStatistics: TeacherStatistics,
  private val solutionDistributor: SolutionDistributor,
  private val gradeTable: GradeTable,
  private val problemStorage: ProblemStorage,
  private val botEventBus: BotEventBus,
) {
  fun assessSolution(
    solution: Solution,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime = LocalDateTime.now(),
  ) {
    gradeTable.assessSolution(solution.id, teacherId, assessment, teacherStatistics, timestamp)
    teacherStatistics.recordAssessment(
      teacherId,
      solution.id,
      LocalDateTime.now(),
      solutionDistributor,
    )
    problemStorage.resolveProblem(solution.problemId).map { problem ->
      botEventBus.publishGradeEvent(
        solution.studentId,
        solution.chatId,
        solution.messageId,
        assessment,
        problem,
      )
    }
  }
}
