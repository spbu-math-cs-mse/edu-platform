package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.database.table.TelegramTechnicalMessagesStorage
import com.github.heheteam.teacherbot.states.createSolutionGradingKeyboard
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TechnicalMessageUpdaterImpl() :
  TechnicalMessageUpdater, TelegramBotController, KoinComponent {
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage by inject()
  private val prettyTechnicalMessageService: PrettyTechnicalMessageService by inject()
  lateinit var myBot: TelegramBot

  override fun updateTechnicalMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>) {
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

  override fun updateTechnnicalMessageInPersonalChat(
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
