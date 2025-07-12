package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

internal interface TeacherStorage {
  fun createTeacher(
    name: String = "defaultName",
    surname: String = "defaultSurname",
    tgId: Long = 0L,
  ): TeacherId

  fun resolveTeacher(teacherId: TeacherId): Result<Teacher, EduPlatformError>

  fun getTeachers(): Result<List<Teacher>, EduPlatformError>

  fun resolveByTgId(tgId: UserId): Result<Teacher?, EduPlatformError>

  fun updateTgId(teacherId: TeacherId, newTgId: UserId): Result<Unit, EduPlatformError>
}
