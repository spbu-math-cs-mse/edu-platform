package com.github.heheteam.adminbot.states

import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnEditDescriptionState() {
  strictlyOn<EditDescriptionState> { state ->
    val message = waitTextMessage().first()
    val answer = message.content.text
    when {
      answer == "/stop" -> StartState(state.context)

      else -> {
        state.course.description = answer
        send(
          state.context,
          "Описание курса ${state.courseName} успешно обновлено",
        )

        StartState(state.context)
      }
    }
  }
}
