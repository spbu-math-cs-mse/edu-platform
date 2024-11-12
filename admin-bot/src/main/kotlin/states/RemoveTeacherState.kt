package com.github.heheteam.adminbot.states

import Teacher
import com.github.heheteam.adminbot.mockTeachers
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveTeacherState() {
  strictlyOn<RemoveTeacherState> { state ->
    val message = waitTextMessage().first()
    val id = message.content.text
    when {
      id == "/stop" -> StartState(state.context)

      !mockTeachers.containsKey(id) -> {
        send(
          state.context,
          "Преподавателя с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        RemoveTeacherState(state.context, state.course, state.courseName)
      }

      else -> {
        if (state.course.teachers.remove(Teacher(id))) {
          send(
            state.context,
            "Преподаватель $id успешно удалён с курса ${state.courseName}",
          )
        } else {
          send(
            state.context,
            "Преподавателя $id нет на курсе ${state.courseName}",
          )
        }
        StartState(state.context)
      }
    }
  }
}
