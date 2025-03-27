package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.studentbot.StudentApi
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class ViewState(override val context: User, val studentId: StudentId) :
  BotState<Unit, Unit, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi) {
    val studentId = studentId
    val studentCourses = getCoursesBulletList(studentId, service)
    bot.send(context, text = studentCourses)
  }

  override fun computeNewState(service: StudentApi, input: Unit): Pair<State, Unit> {
    return Pair(MenuState(context, studentId), Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit

  private fun getCoursesBulletList(studentId: StudentId, studentApi: StudentApi): String {
    val studentCourses = studentApi.getStudentCourses(studentId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (studentCourses.isNotEmpty()) {
      studentCourses.joinToString("\n") { course -> "- " + course.name }
    } else {
      notRegisteredMessage
    }
  }
}
