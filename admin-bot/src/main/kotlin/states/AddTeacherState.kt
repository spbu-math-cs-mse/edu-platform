package com.github.heheteam.adminbot.states

import Teacher
import com.github.heheteam.adminbot.mockTeachers
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddTeacherState() {
  strictlyOn<AddTeacherState> { state ->
    val message = waitTextMessage().first()
    val id = message.content.text
    when {
      id == "/stop" -> StartState(state.context)

      !mockTeachers.containsKey(id) -> {
        send(
          state.context,
          "Преподавателя с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
        )
        AddTeacherState(state.context, state.course, state.courseName)
      }

      state.course.teachers.contains(Teacher(id)) -> {
        send(
          state.context,
          "Преподаватель $id уже есть на курсе ${state.courseName}",
        )
        StartState(state.context)
      }

      else -> {
        state.course.teachers.addLast(Teacher(id))
        send(
          state.context,
          "Преподаватель $id успешно добавлен на курс ${state.courseName}",
        )
        StartState(state.context)
      }
    }
  }
}
