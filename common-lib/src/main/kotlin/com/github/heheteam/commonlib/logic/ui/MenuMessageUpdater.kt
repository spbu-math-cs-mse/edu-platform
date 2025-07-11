package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result

interface MenuMessageUpdater {
  suspend fun updateMenuMessageInPersonalChat(teacherId: TeacherId): Result<Unit, EduPlatformError>

  suspend fun updateMenuMessageInGroupChat(course: Course): Result<Unit, EduPlatformError>
}
