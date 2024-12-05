package com.github.heheteam.parentbot.states

import Dialogues
import Keyboards
import ParentCore
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnChildPerformanceState(
  core: ParentCore,
) {
  strictlyOn<ChildPerformanceState> { state ->
    bot.sendSticker(state.context, Dialogues.nerdSticker)
    bot.send(
      state.context,
      Dialogues.childPerformance(state.child, core),
      replyMarkup = Keyboards.returnBack(),
    )

    waitDataCallbackQuery().first()

    MenuState(state.context, state.parentId)
  }
}
