package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.toScheduledMessageId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

data class QueryMessageIdForDeletionState(override val context: User, val adminId: AdminId) :
  BotStateWithHandlers<String, String, AdminApi> {

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    bot.sendMessage(
      context.id,
      buildEntities { +"Введите ID сообщения, которое вы хотите удалить:" },
    )
    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
  }

  override suspend fun computeNewState(service: AdminApi, input: String): Pair<State, String> =
    binding {
        val messageIdLong =
          input.toLongOrNull().toResultOr { NamedError("Invalid message ID format") }.bind()
        val scheduledMessageId = messageIdLong.toScheduledMessageId()

        val message = service.resolveScheduledMessage(scheduledMessageId).bind()
        if (message.isDeleted) {
          return@binding MenuState(context, adminId) to "Сообщение с таким ID уже удалено."
        }

        ConfirmDeleteMessageState(context, adminId, scheduledMessageId) to
          "Загрузка информации о сообщении..."
      }
      .mapBoth(
        success = { it },
        failure = { error ->
          MenuState(context, adminId) to "Сообщение с таким ID не найдено или уже удалено."
        },
      )

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String,
    input: String,
  ) {
    bot.sendMessage(context.id, response)
  }

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
