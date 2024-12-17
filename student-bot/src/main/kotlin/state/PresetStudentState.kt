package com.github.heheteam.studentbot.state

import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnPresetStudentState() {
  strictlyOn<PresetStudentState> { state ->
    bot.send(
      state.context,
      "Вы --- студент id=${state.student.id} (${state.student.name} ${state.student.surname})",
    )
    MenuState(state.context, state.student.id)
  }
}
