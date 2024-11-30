package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.toStudentId

class InMemoryStudentStorage : StudentStorage {
  var studentId = 0L

  data class Entry(
    val studentId: StudentId,
    val parentId: ParentId,
  )

  val entries = mutableListOf<Entry>()
  override fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ) {
    entries.add(Entry(studentId, parentId))
  }

  override fun getStudents(parentId: ParentId): List<Student> {
    return entries.mapNotNull { if (it.parentId == parentId) Student(it.studentId) else null }
  }

  override fun createStudent(): StudentId = (studentId++).toStudentId()
}
