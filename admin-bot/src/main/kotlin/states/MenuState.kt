package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.Keyboards
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
  strictlyOn<MenuState> { state ->
    bot.send(state.context, Dialogues.menu(), replyMarkup = Keyboards.menu())

    val callback = waitDataCallbackQuery().first()
    val data = callback.data
    answerCallbackQuery(callback)
    when (data) {
      "create course" -> {
        CreateCourseState(state.context)
      }

      "edit course" -> {
        EditCourseState(state.context)
      }

      else -> MenuState(state.context)
    }
  }
}
