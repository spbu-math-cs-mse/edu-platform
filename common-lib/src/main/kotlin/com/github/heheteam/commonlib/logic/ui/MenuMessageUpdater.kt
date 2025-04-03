package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.michaelbull.result.Result

interface MenuMessageUpdater {
  fun updateMenuMessageInPersonalChat(teacherId: TeacherId): Result<Unit, String>

  fun updateMenuMessageInGroupChat(courseId: CourseId): Result<Unit, String>
}
