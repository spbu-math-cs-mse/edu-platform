package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class AskFirstNameState(override val context: User) : BotState<String, Unit, AdminApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: AdminApi,
  ): Result<String, FrontendError> =
    runCatching {
        bot.sendSticker(context, Dialogues.greetingSticker)
        bot.send(context, Dialogues.askFirstName)
        val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
        firstName
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, Unit>, FrontendError> = (AskLastNameState(context, input) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
