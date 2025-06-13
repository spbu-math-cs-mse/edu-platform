package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class PresetTeacherState(override val context: User, private val teacherId: TeacherId) :
  State, BotState<Unit, Result<List<Course>, EduPlatformError>, TeacherApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherApi) = Unit

  override suspend fun computeNewState(
    service: TeacherApi,
    input: Unit,
  ): Pair<State, Result<List<Course>, EduPlatformError>> {
    return Pair(MenuState(context, teacherId), service.getTeacherCourses(teacherId))
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherApi,
    response: Result<List<Course>, EduPlatformError>,
  ) {
    response.mapBoth(
      success = { teacherCourses ->
        val coursesRepr =
          teacherCourses.joinToString("\n") { course: Course ->
            "\u2605 " + course.name + " (id=${course.id})"
          }
        bot.send(context, "Вы --- учитель id=${teacherId}.\nВы преподаете на курсах:\n$coursesRepr")
      },
      failure = {
        bot.send(
          context,
          "Случилась ошибка при обработке запроса ваших курсов: ${it.shortDescription}",
        )
      },
    )
  }
}
