package com.github.heheteam.commonlib

import javax.management.Query
import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatistics

class MockSolutionDistributor(
    private val teacherStatistics: TeacherStatistics = MockTeacherStatistics(),
) : SolutionDistributor {

  private val solutions = ArrayDeque<Solution>()
  private var solutionId = 1

  override fun inputSolution(
    studentId: String,
    solutionContent: SolutionContent,
  ): Solution {
    val solutionType = if (solutionContent.text != null) SolutionType.TEXT else SolutionType.DOCUMENT
    val solution = Solution(
      (solutionId++).toString(),
      Problem("1"),
      solutionContent,
      solutionType
    )
    solutions.add(solution)
    return solution
  }

  override fun querySolution(teacherId: String): Solution? {
    TODO("Not yet implemented")
  }

  override fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
  ) {
    teacherStatistics.recordAssessment(teacherId)
    TODO("Not yet implemented")
  }
}
