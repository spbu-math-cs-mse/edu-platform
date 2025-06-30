package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class PerformDeleteMessageState(
  override val context: User,
  val adminId: AdminId,
  val scheduledMessageId: ScheduledMessageId,
) : BotStateWithHandlers<Unit, String, AdminApi> {

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<Unit>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val result = service.deleteScheduledMessage(scheduledMessageId)
    result.mapBoth(
      success = { bot.sendMessage(context.id, "Сообщение успешно удалено.") },
      failure = { error ->
        bot.sendMessage(
          context.id,
          "Не удалось удалить сообщение. Пожалуйста, попробуйте еще раз.\nОшибка: ${error.shortDescription}",
        )
      },
    )
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Unit,
  ): Result<Pair<State, String>, FrontendError> = Pair(MenuState(context, adminId), "").ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String,
    input: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
