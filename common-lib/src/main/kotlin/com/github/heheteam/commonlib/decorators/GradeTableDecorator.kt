package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder

class GradeTableDecorator(
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

  override fun recordSolutionAssessment(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: kotlinx.datetime.LocalDateTime,
  ) =
    gradeTable.recordSolutionAssessment(solutionId, teacherId, assessment).apply {
      ratingRecorder.updateRating(solutionId)
    }

  override fun isChecked(solutionId: SolutionId): Boolean = gradeTable.isChecked(solutionId)

  override fun getGradingsForSolution(solutionId: SolutionId): List<GradingEntry> =
    gradeTable.getGradingsForSolution(solutionId)
}
