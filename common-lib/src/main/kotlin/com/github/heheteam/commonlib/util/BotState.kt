package com.github.heheteam.commonlib.util

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM

interface BotState<In, Out, HelperService> : State {
  suspend fun readUserInput(bot: BehaviourContext, service: HelperService): In

  suspend fun computeNewState(service: HelperService, input: In): Pair<BotState<*, *, *>, Out>

  suspend fun sendResponse(bot: BehaviourContext, service: HelperService, response: Out)

  suspend fun handle(bot: BehaviourContext, service: HelperService): BotState<*, *, *> {
    val input = readUserInput(bot, service)
    val (newState, response) = computeNewState(service, input)
    sendResponse(bot, service, response)
    return newState
  }
}

inline fun <
  reified S : BotState<*, *, HelperService>,
  HelperService,
> DefaultBehaviourContextWithFSM<BotState<*, *, *>>.registerState(service: HelperService) {
  strictlyOn<S> { state -> state.handle(this, service) }
}
