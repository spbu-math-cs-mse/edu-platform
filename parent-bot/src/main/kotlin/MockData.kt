package com.github.heheteam.samplebot

import Grade
import GradeTable
import Parent
import Problem
import Solution
import SolutionAssessment
import Student
import Teacher

class MockGradeTable(
  val constGradeMap: Map<Student, Map<Problem, Int>> =
    mapOf(
      Student("1") to mapOf(Problem("1c", "1c", "", 1000, "1") to 100, Problem("1d", "1d", "", 1000, "1") to 500, Problem("2a", "2a", "", 1000, "1") to 200),
      Student("2") to mapOf(Problem("1d", "1d", "", 1000, "1") to 250, Problem("2a", "2a", "", 1000, "1") to 200),
      Student("3") to mapOf(Problem("1c", "1c", "", 1000, "1") to 200),
    ),
) : GradeTable {
  override fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  ) {
  }

  override fun getGradeMap(): Map<Student, Map<Problem, Grade>> = constGradeMap
}

var mockTgUsername: String = ""

val mockParents: MutableMap<String, Parent> by lazy {
  mutableMapOf(
    mockTgUsername to Parent("1", listOf(Student("1"), Student("2"), Student("4"))),
    "@somebody" to Parent("2", listOf(Student("3"))),
  )
}
