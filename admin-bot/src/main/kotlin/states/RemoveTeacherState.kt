package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.api.TeacherId
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveTeacherState(core: AdminCore) {
  strictlyOn<RemoveTeacherState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID преподавателя, которого хотите убрать с курса ${state.courseName}"
    }
    val message = waitTextMessage().first()
    val input = message.content.text
    val id = input.toLongOrNull()
    when {
      input == "/stop" -> MenuState(state.context)

      id == null || !core.teacherExists(TeacherId(id)) -> {
        send(
          state.context,
          "Преподавателя с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        RemoveTeacherState(state.context, state.course, state.courseName)
      }

      else -> {
        if (core.removeTeacher(TeacherId(id), state.course.id)) {
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
        MenuState(state.context)
      }
    }
  }
}
