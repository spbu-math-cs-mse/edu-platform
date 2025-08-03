package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage

abstract class SimpleState<ApiService, UserId> :
  BotStateWithHandlersAndUserId<Unit, Unit, ApiService, UserId> {

  val messagesWithKeyboard: MutableList<ContentMessage<*>> = mutableListOf()

  override suspend fun outro(bot: BehaviourContext, service: ApiService) {
    messagesWithKeyboard.forEach { messageWithKeyboard ->
      try {
        bot.editMessageReplyMarkup(messageWithKeyboard, replyMarkup = null)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete inline keyboard", e)
      }
    }
  }

  abstract suspend fun BotContext.run(service: ApiService)

  final override suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlerManager<Unit>,
  ): Result<Unit, FrontendError> =
    runCatching { BotContext(bot, context, updateHandlersController).run(service) }
      .toTelegramError()

  final override suspend fun computeNewState(
    service: ApiService,
    input: Unit,
  ): Result<Pair<State, Unit>, FrontendError> = (defaultState() to Unit).ok()

  final override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ApiService,
    response: Unit,
    input: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
