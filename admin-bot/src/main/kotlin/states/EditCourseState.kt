package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Keyboards
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnEditCourseState(core: AdminCore) {
  strictlyOn<EditCourseState> { state ->
    bot.send(
      state.context,
      "Выберите курс, который хотите изменить:",
      replyMarkup =
      replyKeyboard {
        for ((name, _) in core.getCourses()) {
          row {
            simpleButton(
              text = name,
            )
          }
        }
      },
    )

    val message = waitTextMessage().first()
    val answer = message.content.text

    if (answer == "/stop") {
      return@strictlyOn MenuState(state.context)
    }

    val courseName = answer
    val course = core.getCourse(answer)!!

    bot.send(state.context, "Изменить курс $courseName:", replyMarkup = Keyboards.editCourse())
    val callback = waitDataCallbackQuery().first()
    val action = callback.data

    when (action) {
      Keyboards.returnBack -> StartState(state.context)

      Keyboards.addStudent -> AddStudentState(state.context, course, courseName)

      Keyboards.removeStudent -> RemoveStudentState(state.context, course, courseName)

      Keyboards.addTeacher -> AddTeacherState(state.context, course, courseName)

      Keyboards.removeTeacher -> RemoveTeacherState(state.context, course, courseName)

      Keyboards.editDescription -> EditDescriptionState(state.context, course, courseName)

      Keyboards.addScheduledMessage -> AddScheduledMessageState(state.context, course, courseName)

      else -> EditCourseState(state.context)
    }
  }
}
