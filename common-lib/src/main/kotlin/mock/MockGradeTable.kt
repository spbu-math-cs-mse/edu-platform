package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class MockGradeTable : GradeTable {
  data class GradeTableEntry(
    val student: Student,
    val teacher: Teacher,
    val solution: Solution,
    val solutionAssessment: SolutionAssessment,
  )
  private var mockIncrementalSolutionId = 0L

  private val data: MutableList<GradeTableEntry> =
    mutableListOf()

  override fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  ) {
    data.add(GradeTableEntry(student, teacher, solution, assessment))
  }

  override fun getStudentPerformance(studentId: Long): Map<Problem, Grade> =
    getGradeMap()[studentId] ?: mapOf()

  fun getGradeMap(): Map<Long, Map<Problem, Grade>> {
    val result = mutableMapOf<Long, MutableMap<Problem, Grade>>()
    for (quadruple in data) {
      val student = quadruple.student
      val solution = quadruple.solution
      val solutionAssessment = quadruple.solutionAssessment

      val problem = solution.problem
      val grade = solutionAssessment.grade

      val studentGrades = result.getOrPut(student.id) { mutableMapOf() }
      studentGrades[problem] = grade
    }
    return result
  }

  fun addTrivialAssessment(problem: Problem, studentId: Long, grade: Grade) {
    addAssessment(
      Student(studentId),
      Teacher(0L),
      Solution(
        mockIncrementalSolutionId++,
        studentId,
        RawChatId(0),
        MessageId(0),
        problem,
        SolutionContent(),
        SolutionType.TEXT,
      ),
      SolutionAssessment(grade, ""),
    )
  }

  fun addMockFilling(assignment: Assignment, userId: Long) {
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
