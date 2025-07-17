package com.github.heheteam.studentbot.state.parent

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.registerStateWithParentId
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

inline fun DefaultBehaviourContextWithFSM<State>.registerParentStates(
  parentApi: ParentApi,
  noinline initUpdateHandlers:
    (UpdateHandlersController<() -> Unit, out Any?, FrontendError>, User, ParentId) -> Unit =
    { _, _, _ ->
    },
) {
  registerStateWithParentId<ParentAboutCourseState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentAboutTeachersState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentCourseResultsState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentMenuState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentMethodologyState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentStartQuestState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentAboutKamenetskiState, ParentApi>(parentApi, initUpdateHandlers)
  registerStateWithParentId<ParentAboutMaximovState, ParentApi>(parentApi, initUpdateHandlers)
}
