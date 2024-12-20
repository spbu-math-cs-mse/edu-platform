package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.*

class ParentCore(
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val solutionDistributor: SolutionDistributor,
) {
  fun getChildren(parentId: ParentId): List<Student> = studentStorage.getChildren(parentId)

  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade> = gradeTable.getStudentPerformance(studentId, solutionDistributor)
}
