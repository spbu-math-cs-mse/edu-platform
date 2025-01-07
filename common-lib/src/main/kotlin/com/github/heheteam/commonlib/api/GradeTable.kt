package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import java.time.LocalDateTime

// bound to a course
interface GradeTable {
  // maps student problem ids to grades
  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade>

  // maps student problem ids to grades
  fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: List<AssignmentId>,
  ): Map<ProblemId, Grade>

  // maps student problem ids to grades
  fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade>>

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime = LocalDateTime.now(),
  )

  fun isChecked(solutionId: SolutionId): Boolean
}
