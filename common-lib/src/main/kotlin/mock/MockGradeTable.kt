package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class MockGradeTable : GradeTable {
  data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )
  var mockIncrementalSolutionId = 0

  private val data: MutableList<Quadruple<Student, Teacher, Solution, SolutionAssessment>> =
    mutableListOf()

  override fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  ) {
    data.add(Quadruple(student, teacher, solution, assessment))
  }

  override fun getStudentPerformance(studentId: String): Map<Problem, Grade> =
    getGradeMap()[studentId] ?: mapOf()

  fun getGradeMap(): Map<String, Map<Problem, Grade>> {
    val result = mutableMapOf<String, MutableMap<Problem, Grade>>()
    for (quadruple in data) {
      val student = quadruple.first
      val solution = quadruple.third
      val solutionAssessment = quadruple.fourth

      val problem = solution.problem
      val grade = solutionAssessment.grade

      val studentGrades = result.getOrPut(student.id) { mutableMapOf() }
      studentGrades[problem] = grade
    }
    return result
  }

  fun addTrivialAssessment(problem: Problem, studentId: String, grade: Grade) {
    addAssessment(
      Student(studentId),
      Teacher("0"),
      Solution(
        (mockIncrementalSolutionId++).toString(),
        "",
        RawChatId(0),
        MessageId(0),
        problem,
        SolutionContent(),
        SolutionType.TEXT,
      ),
      SolutionAssessment(grade, ""),
    )
  }

  fun addMockFilling(assignment: Assignment, userId: String) {
    val problems = assignment.problems
    problems.withIndex().filter { it.index % 2 == 1 }
      .forEach { (index, problem) ->
        val grade = if (index == 1) 1 else 0
        addTrivialAssessment(
          problem,
          userId,
          grade,
        )
      }
  }
}