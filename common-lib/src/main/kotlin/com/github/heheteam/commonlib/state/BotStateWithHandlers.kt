package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

interface BotStateWithHandlers<In, Out, ApiService> : State {
  override val context: User

  suspend fun outro(bot: BehaviourContext, service: ApiService)

  suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlersController<() -> Unit, In, Any>,
  )

  fun computeNewState(service: ApiService, input: In): Pair<State, Out>

  suspend fun sendResponse(bot: BehaviourContext, service: ApiService, response: Out)

  suspend fun handle(
    bot: BehaviourContext,
    service: ApiService,
    initUpdateHandlers: (UpdateHandlersController<() -> Unit, In, Any>) -> Unit = {},
  ): State {
    val updateHandlersController = UpdateHandlersController<() -> Unit, In, Any>()
    initUpdateHandlers(updateHandlersController)
    intro(bot, service, updateHandlersController)
    while (true) {
      when (val handlerResult = updateHandlersController.processNextUpdate(bot, context.id)) {
        is ActionWrapper<() -> Unit> -> handlerResult.action.invoke()
        is HandlingError<Any> -> {
          bot.send(context.id, handlerResult.toString())
        }
        is NewState -> return handlerResult.state.also { outro(bot, service) }
        is UserInput<In> -> {
          val (state, response) = computeNewState(service, handlerResult.input)
          sendResponse(bot, service, response)
          outro(bot, service)
          return state
        }
      }
    }
  }
}

inline fun <
  reified S : BotStateWithHandlers<*, *, HelperService>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerState(
  service: HelperService,
  noinline initUpdateHandlers: (UpdateHandlersController<() -> Unit, out Any?, Any>) -> Unit = {},
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}
