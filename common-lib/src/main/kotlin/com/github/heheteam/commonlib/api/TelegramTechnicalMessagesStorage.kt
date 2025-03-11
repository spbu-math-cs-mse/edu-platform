package com.github.heheteam.commonlib.api

import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

data class TelegramMessageInfo(val chatId: RawChatId, val messageId: MessageId)

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

  fun resolveTeacherMenuMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String>
  fun resolveTeacherFirstUncheckedSolutionMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String>
}
