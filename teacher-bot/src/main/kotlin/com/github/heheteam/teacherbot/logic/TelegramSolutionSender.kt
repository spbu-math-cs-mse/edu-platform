package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.database.table.TelegramMessageInfo
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

interface TelegramSolutionSender {
  fun sendPersonalSolutionNotification(
    teacherTgId: RawChatId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String>
}
