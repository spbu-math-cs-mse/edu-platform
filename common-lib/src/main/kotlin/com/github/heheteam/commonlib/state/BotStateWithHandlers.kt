package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

typealias SuspendableBotAction = suspend BehaviourContext.() -> Unit

typealias UpdateHandlersControllerDefault<In> =
  UpdateHandlersController<SuspendableBotAction, In, FrontendError>

interface BotStateWithHandlers<In, Out, ApiService> : State {
  override val context: User

  suspend fun outro(bot: BehaviourContext, service: ApiService)

  suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlersControllerDefault<In>,
  ): Result<Unit, FrontendError>

  suspend fun computeNewState(
    service: ApiService,
    input: In,
  ): Result<Pair<State, Out>, FrontendError>

  suspend fun sendResponse(
    bot: BehaviourContext,
    service: ApiService,
    response: Out,
    input: In,
  ): Result<Unit, FrontendError>

  fun defaultState(): State

  suspend fun handle(
    bot: BehaviourContext,
    service: ApiService,
    initUpdateHandlers: (UpdateHandlersControllerDefault<out Any?>, context: User) -> Unit =
      { _, _ ->
      },
  ): State {
    val updateHandlersController = UpdateHandlersControllerDefault<In>()
    initUpdateHandlers(updateHandlersController, context)
    return handleWithUpdateManager(bot, service, updateHandlersController)
  }

  @Suppress("NestedBlockDepth")
  suspend fun handleWithUpdateManager(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlersController<SuspendableBotAction, In, FrontendError>,
  ): State {
    val introResult = intro(bot, service, updateHandlersController)
    val introError = introResult.getError()
    if (introError != null) {
      if (!introError.shouldBeIgnored) bot.send(context, introError.toMessageText())
      return defaultState()
    }
    while (true) {
      when (val handlerResult = updateHandlersController.processNextUpdate(bot, context.id)) {
        is ActionWrapper<SuspendableBotAction> -> handlerResult.action.invoke(bot)
        is HandlingError<FrontendError> -> {
          if (!handlerResult.error.shouldBeIgnored)
            bot.send(context, handlerResult.error.toMessageText())
        }

        is NewState -> {
          outro(bot, service)
          return handlerResult.state
        }

        is UserInput<In> -> {
          val state = coroutineBinding {
            val (state, response) = computeNewState(service, handlerResult.input).bind()
            sendResponse(bot, service, response, handlerResult.input).bind()
            outro(bot, service)
            state
          }
          return if (state.isErr) {
            if (!state.error.shouldBeIgnored) bot.send(context, state.error.toMessageText())
            outro(bot, service)
            defaultState()
          } else state.value
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
      UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>, context: User,
    ) -> Unit =
    { _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}
