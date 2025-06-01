package com.github.heheteam.commonlib.state

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM

interface BotState<In, Out, HelperService> : State {
  suspend fun readUserInput(bot: BehaviourContext, service: HelperService): In

  fun computeNewState(service: HelperService, input: In): Pair<State, Out>

  suspend fun sendResponse(bot: BehaviourContext, service: HelperService, response: Out)

  suspend fun handle(bot: BehaviourContext, service: HelperService): State {
    val input = readUserInput(bot, service)
    val (newState, response) = computeNewState(service, input)
    sendResponse(bot, service, response)
    return newState
  }
}

inline fun <
  reified S : BotState<*, *, HelperService>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateForBotState(service: HelperService) {
  strictlyOn<S> { state -> state.handle(this, service) }
}
