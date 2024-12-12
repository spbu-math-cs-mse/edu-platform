package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Student
import com.github.michaelbull.result.Result

interface StudentStorage {
  fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ): Result<Unit, BindError<StudentId, ParentId>>

  fun getChildren(parentId: ParentId): List<Student>

  fun createStudent(name: String = "defaultName", surname: String = "defaultSurname", tgId: Long = 0): StudentId

  fun resolveStudent(studentId: StudentId): Result<Student, ResolveError<StudentId>>
}
