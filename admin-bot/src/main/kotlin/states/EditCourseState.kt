package com.github.heheteam.adminbot.states

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.utils.newLine
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnEditCourseState() {
  strictlyOn<EditCourseState> { state ->
    val callback = waitDataCallbackQuery().first()
    val data = callback.data
    answerCallbackQuery(callback)
    when {
      data == "cancel" -> StartState(state.context)

      data == "add a student" -> {
        send(
          state.context,
          "Введите ID ученика, которого хотите добавить на курс ${state.courseName}",
        )
        AddStudentState(state.context, state.course, state.courseName)
      }

      data == "remove a student" -> {
        send(
          state.context,
        ) {
          +"Введите ID ученика, которого хотите убрать с курса ${state.courseName}"
        }
        RemoveStudentState(state.context, state.course, state.courseName)
      }

      data == "add a teacher" -> {
        send(
          state.context,
        ) {
          +"Введите ID преподавателя, которого хотите добавить на курс ${state.courseName}"
        }
        AddTeacherState(state.context, state.course, state.courseName)
      }

      data == "remove a teacher" -> {
        send(
          state.context,
        ) {
          +"Введите ID преподавателя, которого хотите убрать с курса ${state.courseName}"
        }
        RemoveTeacherState(state.context, state.course, state.courseName)
      }

      data == "edit description" -> {
        send(
          state.context,
        ) {
          +"Введите новое описание курса ${state.courseName}. Текущее описание:" + newLine + newLine
          +state.course.description
        }
        EditDescriptionState(state.context, state.course, state.courseName)
      }

      data == "add scheduled message" -> {
        send(
          state.context,
        ) {
          +"Введите сообщение"
        }
        AddScheduledMessageState(state.context, state.course, state.courseName)
      }

      else -> EditCourseState(state.context, state.course, state.courseName)
    }
  }
}
