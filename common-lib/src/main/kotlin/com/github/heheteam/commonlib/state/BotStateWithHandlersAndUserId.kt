package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
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

interface BotStateWithHandlersAndUserId<In, Out, ApiService, UserId> : State {
  override val context: User
  val userId: UserId

  suspend fun outro(bot: BehaviourContext, service: ApiService)

  /**
   * This function must print all the necessary information to the user (including the keyboard or
   * another input query message) and register the callback handler
   */
  suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlersController<() -> Unit, In, NumberedError>,
  ): Result<Unit, NumberedError>

  suspend fun computeNewState(
    service: ApiService,
    input: In,
  ): Result<Pair<State, Out>, NumberedError>

  /** The state to fallback to in case of an error */
  fun defaultState(): State

  suspend fun sendResponse(
    bot: BehaviourContext,
    service: ApiService,
    response: Out,
  ): Result<Unit, NumberedError>

  @Suppress("ReturnCount") // still readable, so no problem
  suspend fun handle(
    bot: BehaviourContext,
    service: ApiService,
    initUpdateHandlers:
      (
        UpdateHandlersController<() -> Unit, In, NumberedError>, context: User, userId: UserId,
      ) -> Unit =
      { _, _, _ ->
      },
  ): State {
    val updateHandlersController = UpdateHandlersController<() -> Unit, In, NumberedError>()
    initUpdateHandlers(updateHandlersController, context, userId)
    val introResult = intro(bot, service, updateHandlersController)
    val introError = introResult.getError()
    if (introError != null) {
      bot.send(context, introError.toMessageText())
      return defaultState()
    }
    while (true) {
      when (val handlerResult = updateHandlersController.processNextUpdate(bot, context.id)) {
        is ActionWrapper<() -> Unit> -> handlerResult.action.invoke()
        is HandlingError<NumberedError> -> {
          bot.send(context, handlerResult.error.toMessageText())
        }

        is NewState -> {
          outro(bot, service)
          return handlerResult.state
        }

        is UserInput<In> -> {
          val state = coroutineBinding {
            val (state, response) = computeNewState(service, handlerResult.input).bind()
            sendResponse(bot, service, response).bind()
            outro(bot, service)
            state
          }
          return if (state.isErr) {
            bot.send(context, state.error.toMessageText())
            outro(bot, service)
            defaultState()
          } else {
            state.value
          }
        }
      }
    }
  }
}

interface BotStateWithHandlersAndStudentId<In, Out, ApiService> :
  BotStateWithHandlersAndUserId<In, Out, ApiService, StudentId>

interface BotStateWithHandlersAndAdminId<In, Out, ApiService> :
  BotStateWithHandlersAndUserId<In, Out, ApiService, AdminId>

inline fun <
  reified S : BotStateWithHandlersAndUserId<*, *, HelperService, StudentId>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateWithStudentId(
  service: HelperService,
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<() -> Unit, out Any?, NumberedError>,
      context: User,
      studentId: StudentId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}

interface BotStateWithHandlersAndTeacherId<In, Out, ApiService> :
  BotStateWithHandlersAndUserId<In, Out, ApiService, TeacherId>

inline fun <
  reified S : BotStateWithHandlersAndUserId<*, *, HelperService, TeacherId>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateWithTeacherId(
  service: HelperService,
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<() -> Unit, out Any?, NumberedError>,
      context: User,
      teacherId: TeacherId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}

inline fun <
  reified S : BotStateWithHandlersAndUserId<*, *, HelperService, UserId>,
  HelperService,
  UserId,
> DefaultBehaviourContextWithFSM<State>.registerStateForBotStateWithHandlersAndUserId(
  service: HelperService,
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<() -> Unit, out Any?, NumberedError>, context: User, userId: UserId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}
