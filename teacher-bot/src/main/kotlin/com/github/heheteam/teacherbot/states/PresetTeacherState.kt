package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class PresetTeacherState(override val context: User, private val teacherId: TeacherId) :
  State, BotState<Unit, List<Course>, CoursesDistributor> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor) {}

  override suspend fun computeNewState(
    coursesDistributor: CoursesDistributor,
    input: Unit,
  ): Pair<BotState<*, *, *>, List<Course>> {
    return Pair(MenuState(context, teacherId), coursesDistributor.getTeacherCourses(teacherId))
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    courses: List<Course>,
  ) {
    val coursesRepr =
      courses.joinToString("\n") { course: Course ->
        "\u2605 " + course.name + " (id=${course.id})"
      }
    bot.send(context, "Вы --- учитель id=${teacherId}.\nВы преподаете на курсах:\n$coursesRepr")
  }
}
