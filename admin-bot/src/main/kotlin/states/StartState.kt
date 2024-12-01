package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminId
import com.github.heheteam.commonlib.api.AdminIdRegistry
import com.github.heheteam.commonlib.api.toAdminId
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(userIdRegistry: AdminIdRegistry) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    bot.send(state.context, Dialogues.greetings())

    var id: AdminId? = userIdRegistry.getUserId(state.context.id)
    if (id != null) {
      return@strictlyOn MenuState(state.context)
    }

    bot.send(state.context, Dialogues.askId())
    id =
      waitTextMessage()
        .first()
        .content.text
        .toLongOrNull()
        ?.toAdminId()

    while (id == null) {
      bot.send(state.context, Dialogues.askIdAgain())
      id =
        waitTextMessage()
          .first()
          .content.text
          .toLongOrNull()
          ?.toAdminId()
    }

    return@strictlyOn MenuState(state.context)
  }
}
