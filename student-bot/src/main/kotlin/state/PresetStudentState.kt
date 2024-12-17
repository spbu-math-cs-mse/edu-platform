package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnPresetStudentState(core: StudentCore) {
  strictlyOn<PresetStudentState> { state ->
    val courses = core.getStudentCourses(state.studentId)
    val coursesRepr =
      courses.joinToString("\n") { course: Course -> "\u2605 " + course.name + " (id=${course.id.id})" }
    bot.send(
      state.context,
      "Вы --- студент id=${state.studentId.id}.\nВаши курсы:\n$coursesRepr",
    )
    MenuState(state.context, state.studentId)
  }
}
