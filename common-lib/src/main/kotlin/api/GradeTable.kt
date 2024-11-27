package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.*

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
}
