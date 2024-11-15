package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.Teacher
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveTeacherState(core: AdminCore) {
  strictlyOn<RemoveTeacherState> { state ->
    val message = waitTextMessage().first()
    val id = message.content.text
    when {
      id == "/stop" -> StartState(state.context)

      !core.teachersTable.containsKey(id) -> {
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
