package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId

data class DeveloperOptions(
  val presetStudentId: StudentId? = null,
  val presetTeacherId: TeacherId? = null,
)
