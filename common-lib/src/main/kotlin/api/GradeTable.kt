package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.*
import java.time.LocalDateTime

// bound to a course
interface GradeTable {
  fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  )

  // maps student problem ids to grades
  fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade>

  // maps student problem ids to grades
  fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: List<AssignmentId>,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade>

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime = LocalDateTime.now(),
  )

  fun isChecked(solutionId: SolutionId): Boolean
}
