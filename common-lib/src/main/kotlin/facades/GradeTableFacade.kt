package com.github.heheteam.commonlib.facades

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import java.time.LocalDateTime

class GradeTableFacade(
  private val gradeTable: GradeTable,
  private val ratingRecorder: GoogleSheetsRatingRecorder,
) : GradeTable {
  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> = gradeTable.getStudentPerformance(studentId, solutionDistributor)

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> = gradeTable.getStudentPerformance(studentId, solutionDistributor)

  override fun getCourseRating(
    courseId: CourseId,
    solutionDistributor: SolutionDistributor,
  ): Map<StudentId, Map<ProblemId, Grade>> = gradeTable.getCourseRating(courseId, solutionDistributor)

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime,
  ) = gradeTable.assessSolution(solutionId, teacherId, assessment, gradeTable, teacherStatistics)
    .apply { ratingRecorder.updateRating(solutionId) }

  override fun isChecked(solutionId: SolutionId): Boolean = gradeTable.isChecked(solutionId)
}
