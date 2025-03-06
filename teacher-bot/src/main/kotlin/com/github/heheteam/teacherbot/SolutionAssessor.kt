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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SolutionAssessor(private val botEventBus: BotEventBus) : KoinComponent {
  private val teacherStatistics: TeacherStatistics by inject()
  private val solutionDistributor: SolutionDistributor by inject()
  private val gradeTable: GradeTable by inject()
  private val problemStorage: ProblemStorage by inject()

  fun assessSolution(
    solution: Solution,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime,
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
