package com.github.heheteam.parentbot.states

import com.github.heheteam.commonlib.api.ParentIdRegistry
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.parentbot.Dialogues
import com.github.heheteam.parentbot.Keyboards
import com.github.heheteam.parentbot.ParentCore
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
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

    waitDataCallbackQueryWithUser(state.context.id).first()

    MenuState(state.context)
  }
}
