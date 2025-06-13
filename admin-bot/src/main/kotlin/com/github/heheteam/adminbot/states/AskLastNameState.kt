package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class AskLastNameState(override val context: User, private val firstName: String) :
  BotStateWithHandlers<AdminId, Unit, AdminApi> {
  override fun computeNewState(service: AdminApi, input: AdminId): Pair<State, Unit> {
    return MenuState(context, input) to Unit
  }

  override fun defaultState(): State {
    return StartState(context)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: AdminId,
  ) = Unit

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<AdminId>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    bot.send(context, Dialogues.askLastName(firstName), replyMarkup = AdminKeyboards.returnBack())
    updateHandlersController.addTextMessageHandler { message ->
      val lastName = message.content.text
      val adminId = service.createAdmin(firstName, lastName, context.id.chatId.long).get()
      if (adminId == null) {
        bot.send(context, Dialogues.notFoundInWhitelist(context.id.chatId.long))
        NewState(StartState(context))
      } else {
        bot.send(context, Dialogues.niceToMeetYou(firstName, lastName))
        NewState(MenuState(context, adminId))
      }
    }
    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.RETURN_BACK) {
        NewState(AskFirstNameState(context))
      } else {
        Unhandled
      }
    }
  }
}
