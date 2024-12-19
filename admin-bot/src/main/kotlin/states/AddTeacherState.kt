package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddTeacherState(core: AdminCore) {
  strictlyOn<AddTeacherState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID преподавателей (через запятую), которых хотите добавить на курс ${state.courseName}, или отправьте /stop, чтобы отменить операцию."
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
            "Преподавателя с идентификатором $input не существует. Попробуйте ещё раз!",
          )
        }

        else -> {
          send(
            state.context,
            "Sorry, not implemented",
          )
        }
      }
    }
    MenuState(state.context)
  }
}
