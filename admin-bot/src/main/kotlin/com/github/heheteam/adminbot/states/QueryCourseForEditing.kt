package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

data class QueryCourseForEditing(override val context: User, val adminIdPrime: AdminId) :
  QueryCourseState(context, adminIdPrime), BotStateWithHandlers<Course?, Unit, AdminApi> {

  override suspend fun computeNewState(service: AdminApi, input: Course?): Pair<State, Unit> =
    if (input != null) EditCourseState(context, adminId, input) to Unit
    else {
      MenuState(context, adminId) to Unit
    }
}
