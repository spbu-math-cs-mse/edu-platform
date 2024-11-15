package com.github.heheteam.commonlib

class MockGradeTable : GradeTable {
  data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )

  private val data: MutableList<Quadruple<Student, Teacher, Solution, SolutionAssessment>> = mutableListOf()

  override fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  ) {
    data.add(Quadruple(student, teacher, solution, assessment))
  }

  override fun getGradeMap(): Map<Student, Map<Problem, Grade>> {
    val result = mutableMapOf<Student, MutableMap<Problem, Grade>>()
    for (quadruple in data) {
      val student = quadruple.first
      val solution = quadruple.third
      val solutionAssessment = quadruple.fourth

      val problem = solution.problem
      val grade = solutionAssessment.grade

      val studentGrades = result.getOrPut(student) { mutableMapOf() }
      studentGrades[problem] = grade
    }
    return result
  }
}
