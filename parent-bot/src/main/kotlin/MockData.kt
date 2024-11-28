package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*

class MockGradeTable(
  val constGradeMap: Map<StudentId, Map<Problem, Int>> =
    mapOf(
      StudentId(1L) to
        mapOf(
          Problem(ProblemId(1L), "1c", "", 1000, AssignmentId(1L)) to 100,
          Problem(ProblemId(2L), "1d", "", 1000, AssignmentId(1L)) to 500,
          Problem(ProblemId(3L), "2a", "", 1000, AssignmentId(1L)) to 200,
        ),
      StudentId(2L) to
        mapOf(
          Problem(ProblemId(2L), "1d", "", 1000, AssignmentId(1L)) to 250,
          Problem(ProblemId(3L), "2a", "", 1000, AssignmentId(1L)) to 200,
        ),
      StudentId(3L) to mapOf(Problem(ProblemId(1L), "1c", "", 1000, AssignmentId(1L)) to 200),
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
  ): Map<ProblemId, Grade> = constGradeMap[studentId]?.mapKeys { it.key.id } ?: mapOf()
}

var mockTgUsername: String = ""

val mockParents: MutableMap<String, Parent> by lazy {
  mutableMapOf(
    mockTgUsername to
      Parent(
        ParentId(1L),
        listOf(Student(StudentId(1L)), Student(StudentId(2L)), Student(StudentId(4L))),
      ),
    "@somebody" to Parent(ParentId(2L), listOf(Student(StudentId(3L)))),
  )
}
