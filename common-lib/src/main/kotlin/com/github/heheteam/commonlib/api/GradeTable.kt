package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.ProblemGrade.Graded
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class GradingEntry(
  val teacherId: TeacherId,
  val assessment: SolutionAssessment,
  val timestamp: kotlinx.datetime.LocalDateTime,
)

sealed class ProblemGrade(open val grade: Grade?) {
  data object Unsent : ProblemGrade(null)

  data object Unchecked : ProblemGrade(null)

  data class Graded(override val grade: Grade) : ProblemGrade(grade)
}

fun Int.toGraded() = Graded(this)

// bound to a course
interface GradeTable {
  // maps student problem ids to grades
  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade?>

  /**
   * Retrieves the grades of a student for the specified assignments.
   *
   * @param studentId The unique identifier of the student.
   * @param assignmentId An identifier of the assignment to fetch grades for.
   * @return A map where each problem ID is associated with its corresponding grade. A grade is
   *   `null` if the solution has not been checked.
   */
  fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
  ): List<Pair<Problem, ProblemGrade>>

  /**
   * Retrieves the grades for all students in the specified course.
   *
   * @param courseId The unique identifier of the course.
   * @return A map where each student ID is associated with a map of problem IDs to their
   *   corresponding grades. A grade is `null` if the solution has not been checked.
   */
  fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>>

  fun recordSolutionAssessment(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: kotlinx.datetime.LocalDateTime = LocalDateTime.now().toKotlinLocalDateTime(),
  )

  fun isChecked(solutionId: SolutionId): Boolean

  fun getGradingsForSolution(solutionId: SolutionId): List<GradingEntry>
}
