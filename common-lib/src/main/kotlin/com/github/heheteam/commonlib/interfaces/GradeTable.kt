package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.ProblemGrade.Graded
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class GradingEntry(
  val teacherId: TeacherId,
  val assessment: SubmissionAssessment,
  val timestamp: kotlinx.datetime.LocalDateTime,
)

sealed class ProblemGrade(open val grade: Grade?) {
  data object Unsent : ProblemGrade(null)

  data object Unchecked : ProblemGrade(null)

  data class Graded(override val grade: Grade) : ProblemGrade(grade)
}

fun Int.toGraded() = Graded(this)

internal interface GradeTable {
  // maps student problem ids to grades
  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade?>

  /**
   * Retrieves the grades of a student for the specified assignments.
   *
   * @param studentId The unique identifier of the student.
   * @param assignmentId An identifier of the assignment to fetch grades for.
   * @return A map where each problem ID is associated with its corresponding grade. A grade is
   *   `null` if the submission has not been checked.
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
   *   corresponding grades. A grade is `null` if the submission has not been checked.
   */
  fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>>

  fun recordSubmissionAssessment(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    assessment: SubmissionAssessment,
    timestamp: kotlinx.datetime.LocalDateTime = LocalDateTime.now().toKotlinLocalDateTime(),
  )

  fun isChecked(submissionId: SubmissionId): Boolean

  fun getGradingsForSubmission(submissionId: SubmissionId): List<GradingEntry>
}
