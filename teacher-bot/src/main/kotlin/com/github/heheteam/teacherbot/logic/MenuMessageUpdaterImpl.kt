package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TelegramMessageInfo
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
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
              Dialogues.menu(),
              replyMarkup = Keyboards.menu(),
            )
          } else {
            myBot.send(chatId.toChatId(), Dialogues.menu(), replyMarkup = Keyboards.menu())
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
