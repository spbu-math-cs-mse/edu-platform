package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage

class DatabaseStudentStorage : StudentStorage {
  override fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ) {
    TODO("Not yet implemented")
  }

  override fun getStudents(parentId: ParentId): List<Student> {
    TODO("Not yet implemented")
  }
}