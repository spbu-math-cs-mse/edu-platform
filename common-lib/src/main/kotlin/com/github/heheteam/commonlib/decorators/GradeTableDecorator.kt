package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime

internal class GradeTableDecorator(
  private val gradeTable: GradeTable,
  private val ratingRecorder: GoogleSheetsRatingRecorder,
) : GradeTable {
  override fun getStudentPerformance(
    studentId: StudentId
  ): Result<Map<ProblemId, Grade?>, EduPlatformError> = gradeTable.getStudentPerformance(studentId)

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
  ): Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError> =
    gradeTable.getStudentPerformance(studentId, assignmentId)

  override fun getCourseRating(
    courseId: CourseId
  ): Result<Map<StudentId, Map<ProblemId, Grade?>>, EduPlatformError> =
    gradeTable.getCourseRating(courseId)

  override fun recordSubmissionAssessment(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    assessment: SubmissionAssessment,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> =
    gradeTable.recordSubmissionAssessment(submissionId, teacherId, assessment).apply {
      ratingRecorder.updateRating(submissionId)
    }

  override fun getGradingsForSubmission(
    submissionId: SubmissionId
  ): Result<List<GradingEntry>, EduPlatformError> =
    gradeTable.getGradingsForSubmission(submissionId)
}
