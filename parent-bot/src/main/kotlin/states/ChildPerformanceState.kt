package com.github.heheteam.parentbot.states

import Dialogues
import Keyboards
import ParentCore
import com.github.heheteam.commonlib.api.ParentIdRegistry
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnChildPerformanceState(
  userIdRegistry: ParentIdRegistry,
  core: ParentCore,
) {
  strictlyOn<ChildPerformanceState> { state ->
    val userId = userIdRegistry.getUserId(state.context.id) ?: return@strictlyOn StartState(state.context)

    bot.sendSticker(state.context, Dialogues.nerdSticker)
    bot.send(
      state.context,
      Dialogues.childPerformance(state.child, core),
      replyMarkup = Keyboards.returnBack(),
    )

    waitDataCallbackQuery().first()

    MenuState(state.context)
  }
}
