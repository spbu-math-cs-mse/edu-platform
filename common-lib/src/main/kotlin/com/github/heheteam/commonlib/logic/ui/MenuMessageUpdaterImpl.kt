package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private object Dialogues {
  const val menu: String = "\u2705 Главное меню"
}

private object Keyboards {
  const val SIGN_UP = "signUpForCourses"
  const val VIEW = "viewMyCourses"
  const val SEND_SOLUTION = "sendSolution"
  const val RETURN_BACK = "back"
  const val APPLY = "apply"
  const val COURSE_ID = "courseId"
  const val PROBLEM_ID = "problemId"
  const val CHECK_GRADES = "checkGrades"
  const val STUDENT_GRADES = "viewStudentGrades"
  const val TOP_GRADES = "viewTopGrades"
  const val FICTITIOUS = "fictitious"
  const val CHECK_DEADLINES = "deadlines"

  fun menu() =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          row { dataButton("Записаться на курсы", SIGN_UP) }
          row { dataButton("Посмотреть мои курсы", VIEW) }
          row { dataButton("Отправить решение", SEND_SOLUTION) }
          row { dataButton("Проверить успеваемость", CHECK_GRADES) }
          row { dataButton("Посмотреть дедлайны", CHECK_DEADLINES) }
        }
    )
}

class MenuMessageUpdaterImpl(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage
) : MenuMessageUpdater, TelegramBotController {
  lateinit var myBot: TelegramBot

  override fun updateMenuMessageInPersonalChat(teacherId: TeacherId) {
    runBlocking(Dispatchers.IO) {
      coroutineBinding {
        technicalMessageStorage
          .resolveTeacherMenuMessage(teacherId)
          .mapBoth(
            { menuMessages ->
              menuMessages.map { menuMessage ->
                try {
                  myBot.deleteMessage(menuMessage.chatId.toChatId(), menuMessage.messageId)
                } catch (e: CommonRequestException) {
                  KSLog.warning("Menu message has already been deleted:\n$e")
                }
              }
            },
            { KSLog.warning("No menu messages registered") },
          )

        val (chatId, messageId) =
          technicalMessageStorage.resolveTeacherFirstUncheckedSolutionMessage(teacherId).bind()
        val menuMessage =
          if (messageId != null) {
            myBot.reply(
              chatId.toChatId(),
              messageId,
              Dialogues.menu,
              replyMarkup = Keyboards.menu(),
            )
          } else {
            myBot.send(chatId.toChatId(), Dialogues.menu, replyMarkup = Keyboards.menu())
          }

        technicalMessageStorage.updateTeacherMenuMessage(
          TelegramMessageInfo(menuMessage.chat.id.chatId, menuMessage.messageId)
        )
      }
    }
  }

  override fun setTelegramBot(telegramBot: TelegramBot) {
    myBot = telegramBot
  }
}
