package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

data class QueryCourseForAssignmentCreation(override val context: User) :
  QueryCourseState(context), BotStateWithHandlers<Course?, Unit, AdminApi> {
  override fun computeNewState(service: AdminApi, input: Course?): Pair<State, Unit> =
    if (input != null) CreateAssignmentState(context, input) to Unit
    else {
      MenuState(context) to Unit
    }
}
