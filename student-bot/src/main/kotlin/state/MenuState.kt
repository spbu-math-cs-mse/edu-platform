package com.github.heheteam.studentbot.state

import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.menuKeyboard
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
  strictlyOn<MenuState> { state ->
    val initialMessage =
      bot.send(
        state.context,
        text = "Мне нужно...",
        replyMarkup = menuKeyboard(),
      )

    val callback = waitDataCallbackQuery().first()
    when (callback.data) {
      ButtonKey.VIEW -> {
        deleteMessage(initialMessage)
        ViewState(state.context)
      }

      ButtonKey.SIGN_UP -> {
        deleteMessage(initialMessage)
        SignUpState(state.context)
      }

      ButtonKey.SEND_SOLUTION -> {
        deleteMessage(initialMessage)
        SendSolutionState(state.context)
      }

      ButtonKey.CHECK_GRADES -> {
        deleteMessage(initialMessage)
        CheckGradesState(state.context)
      }

      else -> {
        deleteMessage(initialMessage)
        MenuState(state.context)
      }
    }
  }
}
