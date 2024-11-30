package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Student

interface StudentStorage {
  fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  )
  fun getStudents(parentId: ParentId): List<Student>
  fun createStudent(): StudentId
}
