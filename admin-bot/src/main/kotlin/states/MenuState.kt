package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.createAssignment
import com.github.heheteam.adminbot.Keyboards.getProblems
import com.github.heheteam.adminbot.Keyboards.getTeachers
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
  strictlyOn<MenuState> { state ->
    val initialMessage = bot.send(state.context, Dialogues.menu(), replyMarkup = Keyboards.menu())

    val callback = waitDataCallbackQueryWithUser(state.context.id).first()
    val data = callback.data
    answerCallbackQuery(callback)
    deleteMessage(initialMessage)
    when (data) {
      "create course" -> {
        CreateCourseState(state.context)
      }

      "edit course" -> {
        EditCourseState(state.context)
      }

      getTeachers -> {
        GetTeachersState(state.context)
      }

      getProblems -> {
        GetProblemsState(state.context)
      }

      createAssignment -> {
        CreateAssignmentState(state.context)
      }

      else -> MenuState(state.context)
    }
  }
}
