package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*

class MockGradeTable(
  val constGradeMap: Map<Long, Map<Problem, Int>> =
    mapOf(
      1L to
        mapOf(
          Problem(1L, "1c", "", 1000, 1L) to 100,
          Problem(2L, "1d", "", 1000, 1L) to 500,
          Problem(3L, "2a", "", 1000, 1L) to 200,
        ),
      2L to mapOf(
        Problem(2L, "1d", "", 1000, 1L) to 250,
        Problem(3L, "2a", "", 1000, 1L) to 200,
      ),
      3L to mapOf(Problem(1L, "1c", "", 1000, 1L) to 200),
    ),
) : GradeTable {
  override fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
  }

  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> =
    constGradeMap[studentId]?.mapKeys { it.key.id } ?: mapOf()
}

var mockTgUsername: String = ""

val mockParents: MutableMap<String, Parent> by lazy {
  mutableMapOf(
    mockTgUsername to Parent(
      1L,
      listOf(Student(1L), Student(2L), Student(4L)),
    ),
    "@somebody" to Parent(2L, listOf(Student(3L))),
  )
}
