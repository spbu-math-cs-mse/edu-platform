package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable

class MockGradeTable(
  val constGradeMap: Map<String, Map<Problem, Int>> =
    mapOf(
      "1" to
        mapOf(
          Problem("1c", "1c", "", 1000, "1") to 100,
          Problem("1d", "1d", "", 1000, "1") to 500,
          Problem("2a", "2a", "", 1000, "1") to 200,
        ),
      "2" to mapOf(
        Problem("1d", "1d", "", 1000, "1") to 250,
        Problem("2a", "2a", "", 1000, "1") to 200,
      ),
      "3" to mapOf(Problem("1c", "1c", "", 1000, "1") to 200),
    ),
) : GradeTable {
  override fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  ) {
  }

  override fun getStudentPerformance(studentId: String): Map<Problem, Grade> =
    constGradeMap[studentId] ?: mapOf()
}

var mockTgUsername: String = ""

val mockParents: MutableMap<String, Parent> by lazy {
  mutableMapOf(
    mockTgUsername to Parent(
      "1",
      listOf(Student("1"), Student("2"), Student("4")),
    ),
    "@somebody" to Parent("2", listOf(Student("3"))),
  )
}
