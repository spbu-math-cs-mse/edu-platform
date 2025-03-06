package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.table.TelegramMessageInfo
import com.github.michaelbull.result.Result

interface TelegramSolutionSender {
  fun sendPersonalSolutionNotification(
    teacherId: TeacherId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String>

  fun sendGroupSolutionNotification(
    courseId: CourseId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String>
}
