package com.github.heheteam.samplebot.state

import com.github.heheteam.samplebot.metaData.menuKeyboard
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
  strictlyOn<MenuState> { state ->

    val initialMessage = bot.send(
      state.context,
      text = "Мне нужно...",
      replyMarkup = menuKeyboard(),
    )

    val callback = waitDataCallbackQuery().first()
    when (callback.data) {
      "viewMyCourses" -> {
        deleteMessage(state.context.id, initialMessage.messageId)
        ViewState(state.context)
      }

      "signUpForCourses" -> {
        deleteMessage(state.context.id, initialMessage.messageId)
        SignUpState(state.context)
      }

      else -> {
        MenuState(state.context)
      }
    }
  }
}
