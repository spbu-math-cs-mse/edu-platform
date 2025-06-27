package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.NumberedError
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

interface BotState<In, Out, HelperService> : State {
  override val context: User

  suspend fun readUserInput(
    bot: BehaviourContext,
    service: HelperService,
  ): Result<In, NumberedError>

  suspend fun computeNewState(
    service: HelperService,
    input: In,
  ): Result<Pair<State, Out>, NumberedError>

  suspend fun sendResponse(
    bot: BehaviourContext,
    service: HelperService,
    response: Out,
  ): Result<Unit, NumberedError>

  suspend fun handle(bot: BehaviourContext, service: HelperService): State {
    val inputResult = readUserInput(bot, service)
    val inputError = inputResult.getError()
    if (inputError != null) {
      bot.send(context, inputError.toMessageText())
      return this
    }
    val input = inputResult.value
    val newStateResult = computeNewState(service, input)
    val newStateError = newStateResult.getError()
    if (newStateError != null) {
      bot.send(context, newStateError.toMessageText())
      return this
    }
    val (newState, response) = newStateResult.value
    val sendResponseResult = sendResponse(bot, service, response)
    val sendResponseError = sendResponseResult.getError()
    if (sendResponseError != null) {
      bot.send(context, sendResponseError.toMessageText())
      return this
    }
    return newState
  }
}

inline fun <
  reified S : BotState<*, *, HelperService>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateForBotState(service: HelperService) {
  strictlyOn<S> { state -> state.handle(this, service) }
}
