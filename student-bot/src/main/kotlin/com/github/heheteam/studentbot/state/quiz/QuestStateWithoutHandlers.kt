package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.state.MenuState
import com.github.michaelbull.result.Result
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

abstract class QuestStateWithoutHandlers :
  BotStateWithHandlersAndUserId<String, Unit, StudentApi, StudentId> {
  var messagesWithKeyboard: MutableList<ContentMessage<TextContent>> = mutableListOf()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) {
    messagesWithKeyboard.forEach { messageWithKeyboard ->
      try {
        bot.editMessageReplyMarkup(messageWithKeyboard, replyMarkup = null)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete inline keyboard", e)
      }
    }
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: String,
  ): Result<Pair<State, Unit>, FrontendError> = (defaultState() to Unit).ok()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
