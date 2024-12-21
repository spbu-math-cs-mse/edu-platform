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
      +"Введите ID преподавателя, которого хотите добавить на курс ${state.courseName}"
    }
    val message = waitTextMessageWithUser(state.context.id).first()
    val input = message.content.text
    val id = input.toLongOrNull()
    when {
      input == "/stop" -> MenuState(state.context)

      id == null || !core.teacherExists(TeacherId(id)) -> {
        send(
          state.context,
          "Преподавателя с идентификатором $input не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        AddTeacherState(state.context, state.course, state.courseName)
      }

      else -> {
        core.registerTeacherForCourse(TeacherId(id), state.course.id)
        send(
          state.context,
          "Преподаватель $id успешно добавлен на курс ${state.courseName}",
        )
        MenuState(state.context)
      }
    }
  }
}
