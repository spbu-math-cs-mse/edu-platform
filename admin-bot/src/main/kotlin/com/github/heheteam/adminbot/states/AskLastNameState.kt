package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getOrElse
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class AskLastNameState(override val context: User, private val firstName: String) :
  BotStateWithHandlers<AdminId, Unit, AdminApi> {

  override fun defaultState(): State = StartState(context)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<AdminId>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    bot.send(context, Dialogues.askLastName(firstName), replyMarkup = AdminKeyboards.returnBack())

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.RETURN_BACK) NewState(StartState(context)) else Unhandled
    }

    updateHandlersController.addTextMessageHandler { message ->
      val lastName = message.content.text
      binding {
          val adminId = service.createAdmin(firstName, lastName, context.id.chatId.long).bind()
          UserInput(adminId)
        }
        .getOrElse {
          bot.send(context, Dialogues.notFoundInWhitelist(context.id.chatId.long))
          NewState(StartState(context))
        }
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: AdminId,
  ): Result<Pair<State, Unit>, NumberedError> = (MenuState(context, input) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: AdminId,
  ): Result<Unit, NumberedError> = Unit.ok()
}
