package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Teacher

interface TeacherStorage {
  fun createTeacher(): TeacherId

  fun resolveTeacher(teacherId: TeacherId): Teacher?
}
