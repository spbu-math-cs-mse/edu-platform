package com.github.heheteam.studentbot.state.parent

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.state.registerStateWithParentId
import com.github.heheteam.studentbot.state.SolutionsParentMenuState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

inline fun DefaultBehaviourContextWithFSM<State>.registerParentStates(
  parentApi: ParentApi,
  noinline initUpdateHandlers: (UpdateHandlersControllerDefault<out Any?>, User, ParentId) -> Unit =
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
  registerStateWithParentId<SolutionsParentMenuState, ParentApi>(parentApi, initUpdateHandlers)
}
