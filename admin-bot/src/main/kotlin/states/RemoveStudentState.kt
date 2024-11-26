package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.Student
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveStudentState(core: AdminCore) {
  strictlyOn<RemoveStudentState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID ученика, которого хотите убрать с курса ${state.courseName}"
    }
    val message = waitTextMessage().first()
    val id = message.content.text
    when {
      id == "/stop" -> StartState(state.context)

      !core.studentExists(id) -> {
        send(
          state.context,
          "Ученика с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        RemoveStudentState(state.context, state.course, state.courseName)
      }

      else -> {
        if (state.course.students.remove(Student(id))) {
          send(
            state.context,
            "Ученик $id успешно удалён с курса ${state.courseName}",
          )
        } else {
          send(
            state.context,
            "Ученика $id нет на курсе ${state.courseName}",
          )
        }
        StartState(state.context)
      }
    }
  }
}
