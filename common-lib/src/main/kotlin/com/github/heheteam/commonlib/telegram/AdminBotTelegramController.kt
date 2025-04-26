package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime

interface AdminBotTelegramController {
  suspend fun notifyAdminOnNewMovingDeadlinesRequest(
    chatId: RawChatId,
    studentId: StudentId,
    newDeadline: LocalDateTime,
  )
}
