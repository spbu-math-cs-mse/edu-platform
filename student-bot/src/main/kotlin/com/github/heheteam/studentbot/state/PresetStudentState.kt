package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.studentbot.StudentApi
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

data class PresetStudentState(override val context: User, val studentId: StudentId) : State

fun DefaultBehaviourContextWithFSM<State>.strictlyOnPresetStudentState(core: StudentApi) {
  strictlyOn<PresetStudentState> { state ->
    val courses = core.getStudentCourses(state.studentId)
    val coursesRepr =
      courses.joinToString("\n") { course: Course ->
        "\u2605 " + course.name + " (id=${course.id})"
      }
    bot.send(state.context, "Вы --- студент id=${state.studentId}.\nВаши курсы:\n$coursesRepr")
    MenuState(state.context, state.studentId)
  }
}
