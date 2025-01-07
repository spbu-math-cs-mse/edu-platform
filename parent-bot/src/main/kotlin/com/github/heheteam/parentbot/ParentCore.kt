package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage

class ParentCore(
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
) {
  fun getChildren(parentId: ParentId): List<Student> = studentStorage.getChildren(parentId)

  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade> = gradeTable.getStudentPerformance(
    studentId
  )
}
