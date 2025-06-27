package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.interfaces.AdminId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

data class QueryCourseForEditing(override val context: User, val adminIdPrime: AdminId) :
  QueryCourseState(context, adminIdPrime) {

  override fun getNextState(course: Course?): State =
    if (course != null) EditCourseState(context, adminId, course) else MenuState(context, adminId)
}
