package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.database.table.TelegramMessageInfo
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.teacherbot.states.SolutionGradings
import com.github.heheteam.teacherbot.states.createSolutionGradingKeyboard
import com.github.heheteam.teacherbot.states.createTechnicalMessageContent
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TelegramSolutionSenderImpl : TelegramSolutionSender {
  private var lateInitTeacherBot: TelegramBot? = null

  fun setBot(bot: TelegramBot) {
    lateInitTeacherBot = bot
  }

  override fun sendPersonalSolutionNotification(
    teacherTgId: RawChatId,
    solution: Solution,
  ): Result<TelegramMessageInfo, String> {
    return runBlocking(Dispatchers.IO) {
      lateInitTeacherBot?.run {
        val solutionMessage = sendSolutionContent(teacherTgId.toChatId(), solution.content)
        val technicalMessageContent = createTechnicalMessageContent(SolutionGradings(solution.id))
        val technicalMessage = reply(solutionMessage, technicalMessageContent)
        editMessageReplyMarkup(
          technicalMessage,
          replyMarkup = createSolutionGradingKeyboard(solution.id),
        )
        Ok(TelegramMessageInfo(technicalMessage.chat.id.chatId, technicalMessage.messageId))
      } ?: Err("uninitialized telegram bot")
    }
  }
}
