package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.*

// bound to a course
interface GradeTable {
  fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  )
  fun getStudentPerformance(studentId: String): Map<Problem, Grade>
}
