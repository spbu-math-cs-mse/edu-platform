package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result

interface TelegramSolutionSender {
  fun sendPersonalSolutionNotification(
    teacherId: TeacherId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String>

  fun sendGroupSolutionNotification(
    courseId: CourseId,
    solution: Solution,
  ): Result<TelegramMessageInfo?, String>
}
