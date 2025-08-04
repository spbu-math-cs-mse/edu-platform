package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

abstract class SimpleState<ApiService, UserId> :
  BotStateWithHandlersAndUserId<Unit, Unit, ApiService, UserId> {

  private val messagesToDelete = mutableListOf<AccessibleMessage>()

  fun AccessibleMessage.deleteLater() = messagesToDelete.add(this)

  suspend fun clearMessagesToDelete(bot: BehaviourContext) {
    for (msg in messagesToDelete) {
      bot.delete(msg)
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: ApiService) {
    clearMessagesToDelete(bot)
  }

  abstract suspend fun BotContext.run(service: ApiService)

  override suspend fun intro(
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
