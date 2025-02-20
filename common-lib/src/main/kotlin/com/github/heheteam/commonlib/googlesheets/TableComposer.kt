package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId

internal class TableComposer {
  fun composeTable(
    course: Course,
    problems: List<Problem>,
    assignments: List<Assignment>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ): ComposedTable {
    val sortedProblems =
      problems.sortedWith(compareBy<Problem> { it.assignmentId.id }.thenBy { it.serialNumber })
    val assignmentSizes = mutableMapOf<AssignmentId, Int>()

    for (problem in sortedProblems) {
      assignmentSizes[problem.assignmentId] =
        assignmentSizes[problem.assignmentId]?.let { i -> i + 1 } ?: 1
    }

    return ComposedTable(
      composeHeader(course, assignments, assignmentSizes, sortedProblems) +
        composeGrades(students, sortedProblems, performance),
      listOf(30, null, null) + List<Int?>(sortedProblems.size) { 40 },
    )
  }

  private fun composeHeader(
    course: Course,
    sortedAssignments: List<Assignment>,
    assignmentSizes: MutableMap<AssignmentId, Int>,
    sortedProblems: List<Problem>,
  ) =
    listOf(
      // Row 1
      // Name of the course
      listOf(FormattedCell(course.name, DataType.STRING, 3).centerAlign().bold().borders(2)) +
        // Assignments
        sortedAssignments.map {
          FormattedCell(it.description, DataType.STRING, assignmentSizes[it.id] ?: 0)
            .bold()
            .borders(2)
            .centerAlign()
        },
      // Row 2
      listOf("id", "surname", "name").map { FormattedCell(it, DataType.STRING).bold().borders() } +
        sortedProblems.map {
          FormattedCell(it.number, DataType.STRING).bold().borders().centerAlign()
        },
    )

  private fun composeGrades(
    students: List<Student>,
    sortedProblems: List<Problem>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ) =
    students.map { student ->
      listOf(student.id.id, student.surname, student.name).map {
        FormattedCell(it.toString(), DataType.STRING).borders()
      } +
        sortedProblems
          .asSequence()
          .map { problem ->
            val grades = performance[student.id]
            if (grades != null) gradeToString(problem.id, grades) else ""
          }
          .map { FormattedCell(it, DataType.STRING).topBorder().bottomBorder().centerAlign() }
          .mapIndexed { index, cell ->
            if (
              index == sortedProblems.size - 1 ||
                sortedProblems[index].assignmentId != sortedProblems[index + 1].assignmentId
            ) {
              cell.rightBorder()
            }
            cell
          }
    }

  private fun gradeToString(problemId: ProblemId, grades: Map<ProblemId, Grade?>): String =
    if (grades.containsKey(problemId)) {
      val grade = grades[problemId]
      when {
        grade == null -> "П"
        else -> grade.toString()
      }
    } else {
      ""
    }
}
