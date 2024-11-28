package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddStudentState(core: AdminCore) {
  strictlyOn<AddStudentState> { state ->
    send(
      state.context,
      "Введите ID ученика, которого хотите добавить на курс ${state.courseName}",
    )
    val message = waitTextMessage().first()
    val input = message.content.text
    val id = input.toLongOrNull()
    when {
      input == "/stop" -> StartState(state.context)

      id == null || !core.studentExists(id) -> {
        send(
          state.context,
          "Ученика с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        AddStudentState(state.context, state.course, state.courseName)
      }

      core.studiesIn(id, state.course) -> {
        send(
          state.context,
          "Ученик $id уже есть на курсе ${state.courseName}",
        )
        StartState(state.context)
      }

      else -> {
        core.registerForCourse(id, state.course.id)
        send(
          state.context,
          "Ученик $id успешно добавлен на курс ${state.courseName}",
        )
        StartState(state.context)
      }
    }
  }
}
