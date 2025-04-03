package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private object Dialogues {
  const val MENU: String = "\u2705 Главное меню"
}

class MenuMessageUpdaterImpl(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage
) : MenuMessageUpdater, TelegramBotController {
  private lateinit var myBot: TelegramBot

  override fun updateMenuMessageInPersonalChat(teacherId: TeacherId): Result<Unit, String> =
    runBlocking(Dispatchers.IO) {
      return@runBlocking coroutineBinding {
        val menuMessages = technicalMessageStorage.resolveTeacherMenuMessage(teacherId).bind()
        deleteMenuMessages(menuMessages)

        val (chatId, messageId) =
          technicalMessageStorage.resolveTeacherFirstUncheckedSolutionMessage(teacherId).bind()
        val menuMessage =
          if (messageId != null) {
            myBot.reply(chatId.toChatId(), messageId, Dialogues.MENU)
          } else {
            myBot.send(chatId.toChatId(), Dialogues.MENU)
          }

        technicalMessageStorage.updateTeacherMenuMessage(
          TelegramMessageInfo(menuMessage.chat.id.chatId, menuMessage.messageId)
        )
      }
    }

  override fun updateMenuMessageInGroupChat(courseId: CourseId): Result<Unit, String> =
    runBlocking(Dispatchers.IO) {
      return@runBlocking coroutineBinding {
        val menuMessages = technicalMessageStorage.resolveGroupMenuMessage(courseId).bind()
        deleteMenuMessages(menuMessages)

        val (chatId, messageId) =
          technicalMessageStorage.resolveGroupFirstUncheckedSolutionMessage(courseId).bind()
        val menuMessage =
          if (messageId != null) {
            myBot.reply(chatId.toChatId(), messageId, Dialogues.MENU)
          } else {
            myBot.send(chatId.toChatId(), Dialogues.MENU)
          }

        technicalMessageStorage.updateTeacherMenuMessage(
          TelegramMessageInfo(menuMessage.chat.id.chatId, menuMessage.messageId)
        )
      }
    }

  private suspend fun deleteMenuMessages(menuMessages: List<TelegramMessageInfo>) {
    menuMessages.map { menuMessage ->
      try {
        myBot.deleteMessage(menuMessage.chatId.toChatId(), menuMessage.messageId)
      } catch (e: CommonRequestException) {
        KSLog.warning("Menu message has already been deleted:\n$e")
      }
    }
  }

  override fun setTelegramBot(telegramBot: TelegramBot) {
    myBot = telegramBot
  }
}
