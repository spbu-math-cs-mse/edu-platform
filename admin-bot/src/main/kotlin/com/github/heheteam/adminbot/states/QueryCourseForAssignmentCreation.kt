package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

data class QueryCourseForAssignmentCreation(override val context: User, val adminIdPrime: AdminId) :
  QueryCourseState(context, adminIdPrime) {
  override suspend fun computeNewState(
    service: AdminApi,
    input: Course?,
  ): Result<Pair<State, Unit>, NumberedError> =
    if (input != null) CreateAssignmentState(context, adminId, input) to Unit
      else {
        MenuState(context, adminId) to Unit
      }
      .ok()
}
