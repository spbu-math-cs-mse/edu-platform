package com.github.heheteam.commonlib.decorators

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

internal class GradeTableDecorator(
  private val gradeTable: GradeTable,
  private val ratingRecorder: GoogleSheetsRatingRecorder,
) : GradeTable {
  override fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade?> =
    gradeTable.getStudentPerformance(studentId)

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
  ): List<Pair<Problem, ProblemGrade>> = gradeTable.getStudentPerformance(studentId, assignmentId)

  override fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>> =
    gradeTable.getCourseRating(courseId)

  override fun recordSubmissionAssessment(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    assessment: SubmissionAssessment,
    timestamp: kotlinx.datetime.LocalDateTime,
  ) =
    gradeTable.recordSubmissionAssessment(submissionId, teacherId, assessment).apply {
      ratingRecorder.updateRating(submissionId)
    }

  override fun isChecked(submissionId: SubmissionId): Boolean = gradeTable.isChecked(submissionId)

  override fun getGradingsForSubmission(submissionId: SubmissionId): List<GradingEntry> =
    gradeTable.getGradingsForSubmission(submissionId)
}
