package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Teacher
import com.github.michaelbull.result.Result

interface TeacherStorage {
  fun createTeacher(name: String = "defaultName", surname: String = "defaultSurname", tgId: Long = 0L): TeacherId

  fun resolveTeacher(teacherId: TeacherId): Result<Teacher, ResolveError<TeacherId>>
}
