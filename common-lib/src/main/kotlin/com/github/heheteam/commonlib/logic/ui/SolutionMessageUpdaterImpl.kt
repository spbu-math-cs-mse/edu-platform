package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SolutionMessageUpdaterImpl(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val prettyTechnicalMessageService: PrettyTechnicalMessageService,
) : SolutionMessageUpdater, TelegramBotController {
  lateinit var myBot: TelegramBot

  override fun updateSolutionMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>) {
    runBlocking(Dispatchers.IO) {
      with(myBot) {
        technicalMessageStorage.resolveGroupMessage(solutionId).map { technicalMessage ->
          edit(
            technicalMessage.chatId.toChatId(),
            technicalMessage.messageId,
            prettyTechnicalMessageService.createPrettyDisplayForTechnicalForTechnicalMessage(
              solutionId
            ),
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
            prettyTechnicalMessageService.createPrettyDisplayForTechnicalForTechnicalMessage(
              solutionId
            ),
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
