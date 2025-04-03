package com.github.heheteam.parentbot.states

import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.parentbot.Dialogues
import com.github.heheteam.parentbot.Keyboards
import com.github.heheteam.parentbot.ParentApi
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnChildPerformanceState(core: ParentApi) {
  strictlyOn<ChildPerformanceState> { state ->
    bot.sendSticker(state.context, Dialogues.nerdSticker)
    bot.send(
      state.context,
      Dialogues.childPerformance(state.child, core),
      replyMarkup = Keyboards.returnBack(),
    )

    waitDataCallbackQueryWithUser(state.context.id).first()

    MenuState(state.context, state.parentId)
  }
}
