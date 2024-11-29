package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.toStudentId

class InMemoryStudentStorage : StudentStorage {
  var studentId = 0L
  override fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ) {
    TODO("Not yet implemented")
  }

  override fun getStudents(parentId: ParentId): List<Student> {
    TODO("Not yet implemented")
  }

  override fun createStudent(): StudentId = (studentId++).toStudentId()
}
