package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

data class PresetStudentState(override val context: User, val studentId: StudentId) : State

fun DefaultBehaviourContextWithFSM<State>.strictlyOnPresetStudentState(core: StudentApi) {
  strictlyOn<PresetStudentState> { state ->
    val maybeCourses = core.getStudentCourses(state.studentId)
    maybeCourses.mapBoth(
      success = { courses ->
        val coursesRepr =
          courses.joinToString("\n") { course: Course ->
            "\u2605 " + course.name + " (id=${course.id})"
          }
        bot.send(state.context, "Вы --- студент id=${state.studentId}.\nВаши курсы:\n$coursesRepr")
      },
      failure = {
        bot.send(
          state.context,
          "Случилась ошибка при запросе ваших курсов. Ошибка: ${it.shortDescription}",
        )
      },
    )
    MenuState(state.context, state.studentId)
  }
}
