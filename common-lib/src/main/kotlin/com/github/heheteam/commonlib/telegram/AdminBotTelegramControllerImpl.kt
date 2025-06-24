package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.asEduPlatformError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

private const val MOVE_DEADLINES = "moveDeadlines"

class AdminBotTelegramControllerImpl(val adminBot: TelegramBot) : AdminBotTelegramController {
  override suspend fun notifyAdminOnNewMovingDeadlinesRequest(
    chatId: RawChatId,
    studentId: StudentId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError> =
    com.github.michaelbull.result
      .runCatching {
        adminBot.send(
          chatId.toChatId(),
          "Ученик $studentId просит продлить дедлайны до ${newDeadline.format(deadlineFormat)}.\nХотите продлить?",
          replyMarkup = moveDeadlines(studentId, newDeadline),
        )
      }
      .mapError { it.asEduPlatformError() }
      .map {}

  private fun moveDeadlines(studentId: StudentId, newDeadline: LocalDateTime) = inlineKeyboard {
    row {
      dataButton("➕ Да", "$MOVE_DEADLINES ${studentId.long} $newDeadline")
      dataButton("➖ Нет", "$MOVE_DEADLINES ${studentId.long}")
    }
  }

  private val deadlineFormat =
    LocalDateTime.Format {
      date(
        LocalDate.Format {
          monthName(MonthNames.ENGLISH_ABBREVIATED)
          char(' ')
          dayOfMonth()
        }
      )
      chars(" ")
      time(
        LocalTime.Format {
          hour()
          char(':')
          minute()
        }
      )
    }
}
