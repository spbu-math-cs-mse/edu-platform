package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.asEduPlatformError
import com.github.heheteam.commonlib.errors.toStackedString
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.boldln
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regularln
import dev.inmo.tgbotapi.utils.row
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

private const val MOVE_DEADLINES = "moveDeadlines"
private const val GRANT_ACCESS_TO_CHALLENGE = "accessToChallenge"

class AdminBotTelegramControllerImpl(val adminBot: TelegramBot) : AdminBotTelegramController {
  override suspend fun notifyAdminOnNewMovingDeadlinesRequest(
    chatId: RawChatId,
    studentId: StudentId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError> =
    runCatching {
        adminBot.send(
          chatId.toChatId(),
          "Ученик $studentId просит продлить дедлайны до ${newDeadline.format(deadlineFormat)}.\nХотите продлить?",
          replyMarkup = moveDeadlines(studentId, newDeadline),
        )
      }
      .mapError { it.asEduPlatformError(AdminBotTelegramControllerImpl::class) }
      .map {}

  override suspend fun notifyAdminOnNewChallengeAccessRequest(
    chatId: RawChatId,
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> =
    runCatching {
        adminBot.send(
          chatId.toChatId(),
          "Ученик $studentId просит доступ к челленджам курса $courseId.\nХотите продлить?",
          replyMarkup = grantAccessToChallenge(studentId, courseId),
        )
      }
      .mapError { it.asEduPlatformError(AdminBotTelegramControllerImpl::class) }
      .map {}

  override suspend fun sendErrorInfo(chatId: RawChatId, error: NumberedError) {
    val errorText = error.error.toStackedString()
    val errorMessage = buildEntities {
      boldln("Ошибка №${error.number}")
      regularln(errorText)
    }
    adminBot.send(chatId.toChatId(), errorMessage)
  }

  private fun moveDeadlines(studentId: StudentId, newDeadline: LocalDateTime) = inlineKeyboard {
    row {
      dataButton("➕ Да", "$MOVE_DEADLINES ${studentId.long} $newDeadline")
      dataButton("➖ Нет", "$MOVE_DEADLINES ${studentId.long}")
    }
  }

  private fun grantAccessToChallenge(studentId: StudentId, courseId: CourseId) = inlineKeyboard {
    row {
      dataButton("➕ Да", "$GRANT_ACCESS_TO_CHALLENGE ${studentId.long} ${courseId.long}")
      dataButton("➖ Нет", "$GRANT_ACCESS_TO_CHALLENGE ${studentId.long}")
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
