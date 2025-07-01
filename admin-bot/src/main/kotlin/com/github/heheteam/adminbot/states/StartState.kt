package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
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
  ): Result<Boolean, FrontendError> {
    if (service.tgIdIsInWhitelist(context.id)) {
      bot.sendSticker(context, Dialogues.greetingSticker)
      return true.ok()
    }

    bot.send(
      context,
      Dialogues.adminIdIsNotInWhitelist(context.id.chatId.long),
      replyMarkup = AdminKeyboards.tryAgain(),
    )
    merge(bot.waitTextMessage(), bot.waitDataCallbackQuery()).first()
    return false.ok()
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Boolean,
  ): Result<Pair<State, String?>, FrontendError> =
    if (!input) {
        StartState(context) to null
      } else {
        service
          .loginByTgId(context.id)
          .mapBoth(
            success = { admin: Admin -> MenuState(context, admin.id) },
            failure = { AskFirstNameState(context) },
          ) to Dialogues.greetings
      }
      .ok()

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
