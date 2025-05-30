package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.MenuMessageInfo
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.michaelbull.result.Result

internal interface TelegramTechnicalMessagesStorage {
  fun registerGroupSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun registerPersonalSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun resolveGroupMessage(solutionId: SolutionId): Result<TelegramMessageInfo, EduPlatformError>

  fun resolvePersonalMessage(solutionId: SolutionId): Result<TelegramMessageInfo, EduPlatformError>

  fun updateTeacherMenuMessage(telegramMessageInfo: TelegramMessageInfo)

  fun resolveTeacherMenuMessage(
    teacherId: TeacherId
  ): Result<List<TelegramMessageInfo>, EduPlatformError>

  /**
   * @return TelegramMessageInfo of the menu message if it exists. Otherwise, just returns the chat
   *   id.
   */
  fun resolveTeacherFirstUncheckedSolutionMessage(
    teacherId: TeacherId
  ): Result<MenuMessageInfo, EduPlatformError>

  fun resolveGroupMenuMessage(
    courseId: CourseId
  ): Result<List<TelegramMessageInfo>, EduPlatformError>

  /**
   * @return TelegramMessageInfo of the menu message if it exists. Otherwise, just returns the chat
   *   id.
   */
  fun resolveGroupFirstUncheckedSolutionMessage(
    courseId: CourseId
  ): Result<MenuMessageInfo, EduPlatformError>
}
