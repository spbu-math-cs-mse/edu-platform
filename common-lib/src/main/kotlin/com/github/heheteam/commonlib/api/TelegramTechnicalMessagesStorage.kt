package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.MenuMessageInfo
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.michaelbull.result.Result

interface TelegramTechnicalMessagesStorage {
  fun registerGroupSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun registerPersonalSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun resolveGroupMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String>

  fun resolvePersonalMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String>

  fun updateTeacherMenuMessage(telegramMessageInfo: TelegramMessageInfo)

  fun resolveTeacherMenuMessage(teacherId: TeacherId): Result<List<TelegramMessageInfo>, String>

  /**
   * @return TelegramMessageInfo of the menu message if it exists. Otherwise, just returns the chat
   *   id.
   */
  fun resolveTeacherFirstUncheckedSolutionMessage(
    teacherId: TeacherId
  ): Result<MenuMessageInfo, String>
}
