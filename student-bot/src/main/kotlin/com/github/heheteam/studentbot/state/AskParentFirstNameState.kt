package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class AskParentFirstNameState(override val context: User, private val from: String?) :
  BotState<String, Unit, ParentApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: ParentApi,
  ): Result<String, FrontendError> =
    runCatching {
        bot.sendSticker(context, Dialogues.greetingSticker)
        bot.send(context, Dialogues.greetings)
        bot.send(context, Dialogues.askFirstName)
        val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
        firstName
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: ParentApi,
    input: String,
  ): Result<Pair<State, Unit>, FrontendError> =
    (AskParentLastNameState(context, input, from) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ParentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
