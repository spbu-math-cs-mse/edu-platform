package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

typealias SuspendableBotAction = suspend BehaviourContext.() -> Unit

typealias UpdateHandlerManager<In> =
  UpdateHandlersController<SuspendableBotAction, In, NumberedError>

interface BotStateWithHandlers<In, Out, ApiService> : State {
  override val context: User

  suspend fun outro(bot: BehaviourContext, service: ApiService)

  suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlerManager<In>,
  ): Result<Unit, NumberedError>

  suspend fun computeNewState(service: ApiService, input: In): Pair<State, Out>

  suspend fun sendResponse(bot: BehaviourContext, service: ApiService, response: Out, input: In)

  fun defaultState(): State

  suspend fun handle(
    bot: BehaviourContext,
    service: ApiService,
    initUpdateHandlers:
      (UpdateHandlersController<SuspendableBotAction, In, NumberedError>, context: User) -> Unit =
      { _, _ ->
      },
  ): State {
    val updateHandlersController =
      UpdateHandlersController<SuspendableBotAction, In, NumberedError>()
    initUpdateHandlers(updateHandlersController, context)
    val introError = intro(bot, service, updateHandlersController).getError()
    if (introError != null) {
      bot.send(context, introError.toMessageText())
      return defaultState()
    }
    while (true) {
      when (val handlerResult = updateHandlersController.processNextUpdate(bot, context.id)) {
        is ActionWrapper<SuspendableBotAction> -> handlerResult.action.invoke(bot)
        is HandlingError<NumberedError> -> {
          bot.send(context, handlerResult.error.toMessageText())
        }

        is NewState -> return handlerResult.state.also { outro(bot, service) }
        is UserInput<In> -> {
          val (state, response) = computeNewState(service, handlerResult.input)
          sendResponse(bot, service, response, handlerResult.input)
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
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<SuspendableBotAction, out Any?, NumberedError>, context: User,
    ) -> Unit =
    { _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}
