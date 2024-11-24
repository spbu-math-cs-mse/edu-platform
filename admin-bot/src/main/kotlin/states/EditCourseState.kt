package com.github.heheteam.adminbot.states

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnEditCourseState() {
  strictlyOn<EditCourseState> { state ->
    val action = selectAction(state)
    when (action) {
      "cancel" -> StartState(state.context)
      "add a student" -> {
        AddStudentState(state.context, state.course, state.courseName)
      }
      "remove a student" -> {
        RemoveStudentState(state.context, state.course, state.courseName)
      }
      "add a teacher" -> {
        AddTeacherState(state.context, state.course, state.courseName)
      }
      "remove a teacher" -> {
        RemoveTeacherState(state.context, state.course, state.courseName)
      }
      "edit description" -> {
        EditDescriptionState(state.context, state.course, state.courseName)
      }
      "add scheduled message" -> {
        AddScheduledMessageState(state.context, state.course, state.courseName)
      }
      else -> EditCourseState(state.context, state.course, state.courseName)
    }
  }
}

private suspend fun BehaviourContext.selectAction(state: EditCourseState): String {
  bot.send(
    state.context,
    "Изменить курс ${state.courseName}:",
    replyMarkup =
    inlineKeyboard {
      row {
        dataButton("Добавить ученика", "add a student")
      }
      row {
        dataButton("Убрать ученика", "remove a student")
      }
      row {
        dataButton("Добавить преподавателя", "add a teacher")
      }
      row {
        dataButton("Убрать преподавателя", "remove a teacher")
      }
      row {
        dataButton("Изменить описание", "edit description")
      }
      row {
        dataButton("Добавить отложенное сообщение", "add scheduled message")
      }
      row {
        dataButton("Назад", "cancel")
      }
    },
  )

  val callback = waitDataCallbackQuery().first()
  val data = callback.data
  answerCallbackQuery(callback)
  return data
}
