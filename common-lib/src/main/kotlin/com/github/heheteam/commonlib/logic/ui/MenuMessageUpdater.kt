package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result

interface MenuMessageUpdater {
  fun updateMenuMessageInPersonalChat(teacherId: TeacherId): Result<Unit, EduPlatformError>

  fun updateMenuMessageInGroupChat(courseId: CourseId): Result<Unit, EduPlatformError>
}
