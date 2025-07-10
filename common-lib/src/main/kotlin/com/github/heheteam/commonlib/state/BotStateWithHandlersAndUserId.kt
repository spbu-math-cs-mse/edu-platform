package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

interface BotStateWithHandlersAndUserId<In, Out, ApiService, UserId> :
  BotStateWithHandlers<In, Out, ApiService> {
  override val context: User
  val userId: UserId

  suspend fun handleWithIds(
    bot: BehaviourContext,
    service: ApiService,
    initUpdateHandlers: (UpdateHandlerManager<In>, context: User, userId: UserId) -> Unit =
      { _, _, _ ->
      },
  ): State {
    val updateHandlersController = UpdateHandlerManager<In>()
    initUpdateHandlers(updateHandlersController, context, userId)

    return handleWithUpdateManager(bot, service, updateHandlersController)
  }
}

interface BotStateWithHandlersAndStudentId<In, Out, ApiService> :
  BotStateWithHandlersAndUserId<In, Out, ApiService, StudentId>

inline fun <
  reified S : BotStateWithHandlersAndUserId<*, *, HelperService, StudentId>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateWithStudentId(
  service: HelperService,
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
      context: User,
      studentId: StudentId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handleWithIds(this, service, initUpdateHandlers) }
}

inline fun <
  reified S : BotStateWithHandlersAndUserId<*, *, HelperService, ParentId>,
  HelperService,
> DefaultBehaviourContextWithFSM<State>.registerStateWithParentId(
  service: HelperService,
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
      context: User,
      parentId: ParentId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handleWithIds(this, service, initUpdateHandlers) }
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
      UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
      context: User,
      teacherId: TeacherId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handleWithIds(this, service, initUpdateHandlers) }
}

inline fun <
  reified S : BotStateWithHandlersAndUserId<*, *, HelperService, UserId>,
  HelperService,
  UserId,
> DefaultBehaviourContextWithFSM<State>.registerStateForBotStateWithHandlersAndUserId(
  service: HelperService,
  noinline initUpdateHandlers:
    (
      UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
      context: User,
      userId: UserId,
    ) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<S> { state -> state.handleWithIds(this, service, initUpdateHandlers) }
}
