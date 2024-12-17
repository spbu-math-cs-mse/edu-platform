package com.github.heheteam.teacherbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.teacherbot.TeacherCore
import com.github.heheteam.teacherbot.states.BotState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnPresetTeacherState(
  core: TeacherCore,
) {
  strictlyOn<PresetTeacherState> { state ->
    val courses = core.getAvailableCourses(state.teacherId)
    val coursesRepr =
      courses.joinToString("\n") { course: Course -> "\u2605 " + course.name + " (id=${course.id})" }
    bot.send(
      state.context,
      "Вы --- учитель id=${state.teacherId}.\nВы преподаете на курсах:\n$coursesRepr",
    )
    MenuState(state.context, state.teacherId)
  }
}
