package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result

interface MenuMessageUpdater {
  suspend fun updateMenuMessageInPersonalChat(teacherId: TeacherId): Result<Unit, EduPlatformError>

  suspend fun updateMenuMessageInGroupChat(courseId: CourseId): Result<Unit, EduPlatformError>
}
