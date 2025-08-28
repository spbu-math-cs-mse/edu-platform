package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

class QueryAssignmentForDeleting(
  override val context: User,
  adminIdPrime: AdminId,
  courseId: CourseId,
) : QueryAssignmentState(context, adminIdPrime, courseId) {

  override suspend fun computeNewState(
    service: AdminApi,
    input: Assignment?,
  ): Result<Pair<State, Unit>, FrontendError> {
    if (input != null) {
      service.deleteAssignment(input.id)
    }
    return Ok(MenuState(context, adminId) to Unit)
  }
}
