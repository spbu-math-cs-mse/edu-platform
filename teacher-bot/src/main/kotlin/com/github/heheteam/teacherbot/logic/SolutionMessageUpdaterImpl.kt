package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.heheteam.teacherbot.states.SolutionGradings
import com.github.heheteam.teacherbot.states.createSolutionGradingKeyboard
import com.github.heheteam.teacherbot.states.createTechnicalMessageContent
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SolutionMessageUpdaterImpl(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage
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
