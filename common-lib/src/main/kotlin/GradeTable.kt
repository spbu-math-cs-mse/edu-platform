package com.github.heheteam.commonlib

// bound to a course
interface GradeTable {
  fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  )

  fun getGradeMap(): Map<Student, Map<Problem, Grade>>
}
