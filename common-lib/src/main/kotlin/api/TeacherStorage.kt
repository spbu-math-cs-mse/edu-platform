package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Teacher
import com.github.michaelbull.result.Result

interface TeacherStorage {
  fun createTeacher(): TeacherId

  fun resolveTeacher(teacherId: TeacherId): Result<Teacher, ResolveError<TeacherId>>

  fun getTeachers(): List<Teacher>
}
