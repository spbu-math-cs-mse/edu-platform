package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.Teacher
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddTeacherState(core: AdminCore) {
  strictlyOn<AddTeacherState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID преподавателя, которого хотите добавить на курс ${state.courseName}"
    }
    val message = waitTextMessage().first()
    val input = message.content.text
    val id = input.toLongOrNull()
    when {
      input == "/stop" -> StartState(state.context)

      id == null || !core.teacherExists(id) -> {
        send(
          state.context,
          "Преподавателя с идентификатором $input не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        AddTeacherState(state.context, state.course, state.courseName)
      }

      state.course.teachers.contains(Teacher(id)) -> {
        send(
          state.context,
          "Преподаватель $input уже есть на курсе ${state.courseName}",
        )
        StartState(state.context)
      }

      else -> {
        state.course.teachers.addLast(Teacher(id))
        send(
          state.context,
          "Преподаватель $input успешно добавлен на курс ${state.courseName}",
        )
        StartState(state.context)
      }
    }
  }
}
