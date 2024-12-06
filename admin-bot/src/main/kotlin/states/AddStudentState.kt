package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddStudentState(core: AdminCore) {
  strictlyOn<AddStudentState> { state ->
    send(
      state.context,
      "Введите ID ученика, которого хотите добавить на курс ${state.courseName}",
    )
    val message = waitTextMessageWithUser(state.context.id).first()
    val input = message.content.text
    val id = input.toLongOrNull()
    when {
      input == "/stop" -> MenuState(state.context)

      id == null || !core.studentExists(StudentId(id)) -> {
        send(
          state.context,
          "Ученика с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        AddStudentState(state.context, state.course, state.courseName)
      }

      core.studiesIn(StudentId(id), state.course) -> {
        send(
          state.context,
          "Ученик $id уже есть на курсе ${state.courseName}",
        )
        MenuState(state.context)
      }

      else -> {
        core.registerStudentForCourse(StudentId(id), state.course.id)
        send(
          state.context,
          "Ученик $id успешно добавлен на курс ${state.courseName}",
        )
        MenuState(state.context)
      }
    }
  }
}
