package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.TelegramMessageInfo
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.states.SolutionGradings
import com.github.heheteam.teacherbot.states.createSolutionGradingKeyboard
import com.github.heheteam.teacherbot.states.createTechnicalMessageContent
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.debug
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SolutionMessageUpdaterImpl(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
) : SolutionMessageUpdater, TelegramBotController {
  lateinit var myBot: TelegramBot

  override fun updateSolutionMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>) {
    runBlocking(Dispatchers.IO) {
      with(myBot) {
        technicalMessageStorage.resolveGroupMessage(solutionId).map { technicalMessage ->
          edit(
            technicalMessage.chatId.toChatId(),
            technicalMessage.messageId,
            createTechnicalMessageContent(SolutionGradings(solutionId, gradings)),
          )
          editMessageReplyMarkup(
            technicalMessage.chatId.toChatId(),
            technicalMessage.messageId,
            replyMarkup = createSolutionGradingKeyboard(solutionId),
          )
        }
      }
    }
  }

  override fun updateSolutionMessageInPersonalChat(
    solutionId: SolutionId,
    gradings: List<GradingEntry>,
  ) {
    runBlocking(Dispatchers.IO) {
      with(myBot) {
        technicalMessageStorage.resolvePersonalMessage(solutionId).map { technicalMessage ->
          edit(
            technicalMessage.chatId.toChatId(),
            technicalMessage.messageId,
            createTechnicalMessageContent(SolutionGradings(solutionId, gradings)),
          )
          editMessageReplyMarkup(
            technicalMessage.chatId.toChatId(),
            technicalMessage.messageId,
            replyMarkup = createSolutionGradingKeyboard(solutionId),
          )
        }
      }
    }
  }

  override fun setTelegramBot(telegramBot: TelegramBot) {
    myBot = telegramBot
  }
}


class MenuMessageUpdaterImpl(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
) : MenuMessageUpdater, TelegramBotController {
  lateinit var myBot: TelegramBot

  override fun updateMenuMessageInPersonalChat(solutionId: SolutionId) {
    runBlocking(Dispatchers.IO) {
      coroutineBinding {
        with(myBot) {
          technicalMessageStorage.resolveTeacherMenuMessage(solutionId).map { menuMessage ->
            try{deleteMessage(menuMessage.chatId.toChatId(), menuMessage.messageId)}
            catch (e: CommonRequestException){
              KSLog.warning("Menu message has already been deleted")
              KSLog.warning(e)
            }
          }

          val solutionMessage = technicalMessageStorage.resolveTeacherFirstUncheckedSolutionMessage(solutionId).bind()
          val menuMessage = reply(
            solutionMessage.chatId.toChatId(),
            solutionMessage.messageId,
            Dialogues.menu(),
            replyMarkup = Keyboards.menu()
          )
          technicalMessageStorage.updateTeacherMenuMessage(
            TelegramMessageInfo(
              menuMessage.chat.id.chatId,
              menuMessage.messageId
            )
          )
        }
      }
    }
  }

  override fun setTelegramBot(telegramBot: TelegramBot) {
    myBot = telegramBot
  }
}

