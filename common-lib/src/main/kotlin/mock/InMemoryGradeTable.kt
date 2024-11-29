package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import java.time.LocalDateTime

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

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime,
  ) {
    TODO("Not yet implemented")
  }

  override fun isChecked(solutionId: SolutionId): Boolean {
    TODO("Not yet implemented")
  }

  fun addTrivialAssessment(grade: Grade) {
    addAssessment(
      TeacherId(0L),
      SolutionId(mockIncrementalSolutionId++),
      SolutionAssessment(grade, ""),
    )
  }
}
