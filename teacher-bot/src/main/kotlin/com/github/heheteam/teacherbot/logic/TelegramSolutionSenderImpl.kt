package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.TelegramMessageInfo
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.teacherbot.states.SolutionGradings
import com.github.heheteam.teacherbot.states.createSolutionGradingKeyboard
import com.github.heheteam.teacherbot.states.createTechnicalMessageContent
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TelegramSolutionSenderImpl(private val teacherStorage: TeacherStorage) :
  TelegramSolutionSender, TelegramBotController {
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
          val technicalMessageContent = createTechnicalMessageContent(SolutionGradings(solution.id))
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
          val technicalMessageContent = createTechnicalMessageContent(SolutionGradings(solution.id))
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
