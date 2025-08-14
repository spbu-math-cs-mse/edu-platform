package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

interface StudentStorage {
  fun getAll(): Result<List<Student>, EduPlatformError>

  fun getWithCompletedQuest(): Result<List<Student>, EduPlatformError>

  fun getAdmins(): Result<List<Student>, EduPlatformError>

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

  fun updateSelectedCourse(
    studentId: StudentId,
    selectedCourseId: CourseId?,
  ): Result<Unit, EduPlatformError>
}
