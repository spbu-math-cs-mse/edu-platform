package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.state.BotState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge

class StartState(override val context: User) : BotState<Boolean, String?, AdminApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: AdminApi,
  ): Result<Boolean, FrontendError> = coroutineBinding {
    runCatching {
        if (service.tgIdIsInWhitelist(context.id).bind()) {
          bot.sendSticker(context, Dialogues.greetingSticker)
          return@runCatching true
        }

        bot.send(
          context,
          Dialogues.adminIdIsNotInWhitelist(context.id.chatId.long),
          replyMarkup = AdminKeyboards.tryAgain(),
        )
        merge(bot.waitTextMessage(), bot.waitDataCallbackQuery()).first()
        return@runCatching false
      }
      .toTelegramError()
      .mapError { it as FrontendError }
      .bind()
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Boolean,
  ): Result<Pair<State, String?>, FrontendError> = binding {
    if (!input) {
      StartState(context) to null
    } else {
      val adminOrNull = service.loginByTgId(context.id).bind()
      if (adminOrNull == null) {
        AskFirstNameState(context)
      } else {
        MenuState(context, adminOrNull.id)
      } to Dialogues.greetings
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String?,
  ): Result<Unit, FrontendError> =
    runCatching {
        if (response != null) {
          bot.send(context, response)
        }
      }
      .toTelegramError()
}
