package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TelegramSolutionSenderImpl(
  private val teacherStorage: TeacherStorage,
  private val prettyTechnicalMessageService: PrettyTechnicalMessageService,
) : TelegramSolutionSender, TelegramBotController {
  private var lateInitTeacherBot: TelegramBot? = null

  override fun sendPersonalSolutionNotification(
    teacherId: TeacherId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String> =
    runBlocking(Dispatchers.IO) {
      coroutineBinding {
        val bot = lateInitTeacherBot.toResultOr { "uninitialized telegram bot" }.bind()
        with(bot) {
          val teacher =
            teacherStorage.resolveTeacher(teacherId).mapError { "Failed to resolve teacher" }.bind()
          val solutionMessage = sendSolutionContent(teacher.tgId.toChatId(), solution.content)
          val technicalMessageContent =
            prettyTechnicalMessageService.createPrettyDisplayForTechnicalForTechnicalMessage(
              solution.id
            )
          val technicalMessage = reply(solutionMessage, technicalMessageContent)
          editMessageReplyMarkup(
            technicalMessage,
            replyMarkup = createSolutionGradingKeyboard(solution.id),
          )
          TelegramMessageInfo(technicalMessage.chat.id.chatId, technicalMessage.messageId)
        }
      }
    }

  override fun sendGroupSolutionNotification(
    courseId: CourseId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String> =
    runBlocking(Dispatchers.IO) {
      coroutineBinding {
        val bot = lateInitTeacherBot.toResultOr { "uninitialized telegram bot" }.bind()
        with(bot) {
          val chat =
            registeredGroups[courseId].toResultOr { "no chat registered for $courseId" }.bind()
          val solutionMessage = sendSolutionContent(chat.toChatId(), solution.content)
          val technicalMessageContent =
            prettyTechnicalMessageService.createPrettyDisplayForTechnicalForTechnicalMessage(
              solution.id
            )
          val technicalMessage = reply(solutionMessage, technicalMessageContent)
          editMessageReplyMarkup(
            technicalMessage,
            replyMarkup = createSolutionGradingKeyboard(solution.id),
          )
          TelegramMessageInfo(technicalMessage.chat.id.chatId, technicalMessage.messageId)
        }
      }
    }

  private val registeredGroups = ConcurrentHashMap<CourseId, RawChatId>()

  fun registerGroupForSolution(courseId: CourseId, chatId: RawChatId) {
    registeredGroups[courseId] = chatId
  }

  override fun setTelegramBot(telegramBot: TelegramBot) {
    lateInitTeacherBot = telegramBot
  }
}

@Serializable private data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)

internal fun createSolutionGradingKeyboard(solutionId: SolutionId) =
  InlineKeyboardMarkup(
    keyboard =
      matrix {
        row {
          dataButton("\uD83D\uDE80+", Json.encodeToString(GradingButtonContent(solutionId, 1)))
          dataButton("\uD83D\uDE2D-", Json.encodeToString(GradingButtonContent(solutionId, 0)))
        }
      }
  )
