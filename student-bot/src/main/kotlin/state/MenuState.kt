package com.github.heheteam.studentbot.state

import com.github.heheteam.studentbot.metaData.*
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
      ButtonKey.VIEW -> {
        deleteMessage(state.context.id, initialMessage.messageId)
        ViewState(state.context)
      }

      ButtonKey.SIGN_UP -> {
        deleteMessage(state.context.id, initialMessage.messageId)
        SignUpState(state.context)
      }

      ButtonKey.SEND_SOLUTION -> {
        deleteMessage(state.context.id, initialMessage.messageId)
        SendSolutionState(state.context)
      }

      "checkGrades" -> { // TODO: refactoring
        deleteMessage(state.context.id, initialMessage.messageId)
        CheckGradesState(state.context)
      }

      else -> {
        MenuState(state.context)
      }
    }
  }
}
