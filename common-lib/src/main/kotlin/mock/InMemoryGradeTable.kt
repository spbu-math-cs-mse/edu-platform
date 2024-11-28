package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*

class InMemoryGradeTable : GradeTable {
  data class GradeTableEntry(
    val teacherId: TeacherId,
    val solutionId: SolutionId,
    val solutionAssessment: SolutionAssessment,
  )

  private var mockIncrementalSolutionId = 0L

  private val entries: MutableList<GradeTableEntry> =
    mutableListOf()

  override fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    entries.add(GradeTableEntry(teacherId, solutionId, assessment))
  }

  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> =
    entries
      .filter {
        val solution = solutionDistributor.resolveSolution(it.solutionId)
        solution.studentId == studentId
      }.associate {
        val solution = solutionDistributor.resolveSolution(it.solutionId)
        solution.problemId to it.solutionAssessment.grade
      }

  fun addTrivialAssessment(grade: Grade) {
    addAssessment(
      TeacherId(0L),
      SolutionId(mockIncrementalSolutionId++),
      SolutionAssessment(grade, ""),
    )
  }

  fun addMockFilling(
    assignment: Assignment,
    userId: Long,
  ) {
    val problemIds = assignment.problemIds
    problemIds
      .withIndex()
      .filter { it.index % 2 == 1 }
      .forEach { (index, _) ->
        val grade = if (index == 1) 1 else 0
        addTrivialAssessment(
          grade,
        )
      }
  }
}
