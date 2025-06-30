package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.toNumberedResult
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class PresetTeacherState(override val context: User, private val teacherId: TeacherId) :
  State, BotState<Unit, List<Course>, TeacherApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherApi,
  ): Result<Unit, NumberedError> = Unit.ok()

  override suspend fun computeNewState(
    service: TeacherApi,
    input: Unit,
  ): Result<Pair<State, List<Course>>, NumberedError> = binding {
    Pair(MenuState(context, teacherId), service.getTeacherCourses(teacherId).bind())
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherApi,
    response: List<Course>,
  ): Result<Unit, NumberedError> {
    val coursesRepr =
      response.joinToString("\n") { course: Course ->
        "\u2605 " + course.name + " (id=${course.id})"
      }
    return runCatching {
        bot.send(context, "Вы --- учитель id=${teacherId}.\nВы преподаете на курсах:\n$coursesRepr")
        Unit
      }
      .toNumberedResult()
  }
}
