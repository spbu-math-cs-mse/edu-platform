package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.teacherbot.states.SolutionGradings
import com.github.heheteam.teacherbot.states.createSolutionGradingKeyboard
import com.github.heheteam.teacherbot.states.createTechnicalMessageContent
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TechnicalMessageUpdaterImpl(
  val bot: BehaviourContext,
  val x: TelegramSolutionMessagesHandler,
) : TechnicalMessageUpdater {
  override fun updateTechnicalMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>) {
    runBlocking(Dispatchers.IO) {
      with(bot) {
        x.resolveGroupMessage(solutionId).map { technicalMessage ->
          //          bot.edit(technicalMessage.chatId.toChatId(), technicalMessage.messageId,
          // "edited by bot")
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
}
