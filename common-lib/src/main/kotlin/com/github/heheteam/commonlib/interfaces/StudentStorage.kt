package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.errors.BindError
import com.github.heheteam.commonlib.errors.EduPlatformError
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
    grade: Int?,
    from: String?,
  ): Result<StudentId, EduPlatformError>

  fun resolveStudent(studentId: StudentId): Result<Student?, EduPlatformError>

  fun resolveByTgId(tgId: UserId): Result<Student?, EduPlatformError>

  fun updateTgId(studentId: StudentId, newTgId: UserId): Result<Unit, EduPlatformError>

  fun updateLastQuestState(
    studentId: StudentId,
    lastQuestState: String,
  ): Result<Unit, EduPlatformError>
}
