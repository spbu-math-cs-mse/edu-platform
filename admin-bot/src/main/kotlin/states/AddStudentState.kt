package com.github.heheteam.adminbot.states

import Student
import com.github.heheteam.adminbot.AdminCore
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddStudentState(core: AdminCore) {
  strictlyOn<AddStudentState> { state ->
    val message = waitTextMessage().first()
    val id = message.content.text
    when {
      id == "/stop" -> StartState(state.context)

      !core.studentsTable.containsKey(id) -> {
        send(
          state.context,
          "Ученика с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        AddStudentState(state.context, state.course, state.courseName)
      }

      state.course.students.contains(Student(id)) -> {
        send(
          state.context,
          "Ученик $id уже есть на курсе ${state.courseName}",
        )
        StartState(state.context)
      }

      else -> {
        state.course.students.addLast(Student(id))
        send(
          state.context,
          "Ученик $id успешно добавлен на курс ${state.courseName}",
        )
        StartState(state.context)
      }
    }
  }
}
