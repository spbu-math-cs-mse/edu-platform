package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class ViewState(override val context: User, val studentId: StudentId) :
  BotState<Unit, Unit, CoursesDistributor> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor) {
    val studentId = studentId
    val studentCourses = getCoursesBulletList(studentId, service)
    bot.send(context, text = studentCourses)
  }

  override fun computeNewState(service: CoursesDistributor, input: Unit): Pair<State, Unit> {
    return Pair(MenuState(context, studentId), Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: Unit,
  ) = Unit

  private fun getCoursesBulletList(
    studentId: StudentId,
    coursesDistributor: CoursesDistributor,
  ): String {
    val studentCourses = coursesDistributor.getStudentCourses(studentId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (studentCourses.isNotEmpty()) {
      studentCourses.joinToString("\n") { course -> "- " + course.name }
    } else {
      notRegisteredMessage
    }
  }
}
