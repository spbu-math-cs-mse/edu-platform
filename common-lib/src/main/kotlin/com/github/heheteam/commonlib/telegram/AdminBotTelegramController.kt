package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime

interface AdminBotTelegramController {
  suspend fun notifyAdminOnNewMovingDeadlinesRequest(
    chatId: RawChatId,
    studentId: StudentId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError>

  suspend fun sendErrorInfo(chatId: RawChatId, error: NumberedError)
}
