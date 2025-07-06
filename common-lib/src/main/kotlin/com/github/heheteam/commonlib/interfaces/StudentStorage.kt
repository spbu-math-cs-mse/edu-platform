package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.errors.BindError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

interface StudentStorage {
  fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ): Result<Unit, BindError<StudentId, ParentId>>

  fun getChildren(parentId: ParentId): Result<List<Student>, EduPlatformError>

  fun createStudent(
    name: String = "defaultName",
    surname: String = "defaultSurname",
    tgId: Long = 0L,
  ): Result<StudentId, EduPlatformError>

  fun resolveStudent(studentId: StudentId): Result<Student?, EduPlatformError>

  fun resolveByTgId(tgId: UserId): Result<Student, ResolveError<UserId>>

  fun updateTgId(studentId: StudentId, newTgId: UserId): Result<Unit, ResolveError<StudentId>>
}
