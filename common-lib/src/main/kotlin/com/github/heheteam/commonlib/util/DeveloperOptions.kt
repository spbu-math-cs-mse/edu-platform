package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId

data class DeveloperOptions(
  val presetStudentId: StudentId? = null,
  val presetTeacherId: TeacherId? = null,
)
