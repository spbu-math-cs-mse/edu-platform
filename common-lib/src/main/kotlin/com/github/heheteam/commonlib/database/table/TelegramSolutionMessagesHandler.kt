package com.github.heheteam.commonlib.database.table

import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

data class TelegramMessageInfo(val chatId: RawChatId, val messageId: MessageId)

interface TelegramSolutionMessagesHandler {
  fun registerGroupSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun resolveGroupMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String>
}
