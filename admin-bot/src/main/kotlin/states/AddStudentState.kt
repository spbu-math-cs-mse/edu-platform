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
      "Введите ID учеников (через запятую), которых хотите добавить на курс ${state.courseName}, или отправьте /stop, чтобы отменить операцию.",
    )
    val message = waitTextMessageWithUser(state.context.id).first()
    val input = message.content.text
    if (input == "/stop") {
      return@strictlyOn MenuState(state.context)
    }
    val ids = input.split(",").map { it.trim().toLongOrNull() }
    ids.forEach { id ->
      when {
        id == null || !core.studentExists(StudentId(id)) -> {
          send(
            state.context,
            "Ученика с идентификатором $id не существует. Попробуйте ещё раз!",
          )
        }

        core.studiesIn(StudentId(id), state.course) -> {
          send(
            state.context,
            "Ученик $id уже есть на курсе ${state.courseName}!",
          )
        }

        else -> {
          core.registerStudentForCourse(StudentId(id), state.course.id)
          send(
            state.context,
            "Ученик $id успешно добавлен на курс ${state.courseName}!",
          )
        }
      }
    }
    MenuState(state.context)
  }
}
