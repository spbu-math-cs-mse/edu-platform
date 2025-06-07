package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
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
    updateHandlersController: UpdateHandlersController<() -> Unit, In, Any>,
  ): Result<Unit, EduPlatformError>

  fun computeNewState(service: ApiService, input: In): Pair<State, Out>

  /** The state to fallback to in case of an error */
  fun defaultState(): State

  suspend fun sendResponse(bot: BehaviourContext, service: ApiService, response: Out)

  @Suppress("ReturnCount") // still readable, so no problem
  suspend fun handle(
    bot: BehaviourContext,
    service: ApiService,
    initUpdateHandlers:
      (UpdateHandlersController<() -> Unit, In, Any>, context: User, userId: UserId) -> Unit =
      { _, _, _ ->
      },
  ): State {
    val updateHandlersController = UpdateHandlersController<() -> Unit, In, Any>()
    initUpdateHandlers(updateHandlersController, context, userId)
    val introError = intro(bot, service, updateHandlersController).getError()
    if (introError != null) {
      bot.send(
        context,
        "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n" +
          "Ошибка:${introError.shortDescription}",
      )
      return defaultState()
    }
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
      UpdateHandlersController<() -> Unit, out Any?, Any>, context: User, studentId: StudentId,
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
      UpdateHandlersController<() -> Unit, out Any?, Any>, context: User, teacherId: TeacherId,
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
    (UpdateHandlersController<() -> Unit, out Any?, Any>, context: User, userId: UserId) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handle(this, service, initUpdateHandlers) }
}
