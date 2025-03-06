package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatistics
import java.time.LocalDateTime

class GradeTableDecorator(
  private val gradeTable: GradeTable,
  private val ratingRecorder: RatingRecorder,
) : GradeTable {
  override fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade?> =
    gradeTable.getStudentPerformance(studentId)

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentIds: List<AssignmentId>,
  ): Map<ProblemId, Grade?> = gradeTable.getStudentPerformance(studentId)

  override fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>> =
    gradeTable.getCourseRating(courseId)

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime,
  ) =
    gradeTable.assessSolution(solutionId, teacherId, assessment, teacherStatistics).apply {
      ratingRecorder.updateRating(solutionId)
    }

  override fun isChecked(solutionId: SolutionId): Boolean = gradeTable.isChecked(solutionId)
}
