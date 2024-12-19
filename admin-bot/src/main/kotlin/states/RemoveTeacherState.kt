package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveTeacherState(core: AdminCore) {
  strictlyOn<RemoveTeacherState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID преподавателей (через запятую), которых хотите убрать с курса ${state.courseName}, или отправьте /stop, чтобы отменить операцию."
    }
    val message = waitTextMessageWithUser(state.context.id).first()
    val input = message.content.text
    if (input == "/stop") {
      return@strictlyOn MenuState(state.context)
    }
    val ids = input.split(",").map { it.trim().toLongOrNull() }
    ids.forEach { id ->
      when {
        id == null || !core.teacherExists(TeacherId(id)) -> {
          send(
            state.context,
            "Преподавателя с идентификатором $id не существует. Попробуйте ещё раз!",
          )
        }

        else -> {
          if (core.removeTeacher(TeacherId(id), state.course.id)) {
            send(
              state.context,
              "Преподаватель $id успешно удалён с курса ${state.courseName}!",
            )
          } else {
            send(
              state.context,
              "Преподавателя $id нет на курсе ${state.courseName}!",
            )
          }
        }
      }
    }
    MenuState(state.context)
  }
}
