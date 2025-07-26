package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.toChatId

class AddAdminState(override val context: User, val adminId: AdminId) :
  BotStateWithHandlers<String, String, AdminApi> {
  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<String>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    bot.send(
      context,
      "Введите ID админа, которого хотите добавить.",
      replyMarkup = AdminKeyboards.returnBack(),
    )

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.RETURN_BACK) NewState(MenuState(context, adminId))
      else Unhandled
    }
    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, String>, FrontendError> = coroutineBinding {
    val stringId = input.trim()
    val newAdminId =
      stringId.toLongOrNull()
        ?: return@coroutineBinding this@AddAdminState to "Некорректный id: $stringId"
    service.addTgIdToWhitelist(newAdminId.toChatId())
    return@coroutineBinding MenuState(context, adminId) to
      "Админ с id $newAdminId успешно добавлен в систему!"
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String,
    input: String,
  ) =
    runCatching {
        bot.send(context, response)
        Unit
      }
      .toTelegramError()
}
