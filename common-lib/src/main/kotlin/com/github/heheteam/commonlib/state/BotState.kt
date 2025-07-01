package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.Chat

interface BotState<In, Out, HelperService> : State {
  override val context: Chat

  suspend fun readUserInput(
    bot: BehaviourContext,
    service: HelperService,
  ): Result<In, FrontendError>

  suspend fun computeNewState(
    service: HelperService,
    input: In,
  ): Result<Pair<State, Out>, FrontendError>

  suspend fun sendResponse(
    bot: BehaviourContext,
    service: HelperService,
    response: Out,
  ): Result<Unit, FrontendError>

  suspend fun handle(bot: BehaviourContext, service: HelperService): State {
    val state = coroutineBinding {
      val input = readUserInput(bot, service).bind()
      val (newState, response) = computeNewState(service, input).bind()
      sendResponse(bot, service, response).bind()
      newState
    }
    return if (state.isErr) {
      if (!state.error.shouldBeIgnored) bot.send(context, state.error.toMessageText())
      this
    } else {
      state.value
    }
  }
}

inline fun <
  reified S : BotState<*, *, HelperService>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateForBotState(service: HelperService) {
  strictlyOn<S> { state -> state.handle(this, service) }
}
