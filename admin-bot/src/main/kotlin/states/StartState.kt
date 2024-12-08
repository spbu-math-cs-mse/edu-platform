package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminIdRegistry
import com.github.heheteam.commonlib.api.toAdminId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(adminIdRegistry: AdminIdRegistry) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    bot.send(state.context, Dialogues.greetings())

    val result = adminIdRegistry.getUserId(state.context.id)
    if (result.isErr) {
      return@strictlyOn MenuState(state.context)
    }

    bot.send(state.context, Dialogues.devAskForId())
    var id =
      waitTextMessageWithUser(state.context.id)
        .first()
        .content.text
        .toLongOrNull()
        ?.toAdminId()

    while (id == null) {
      bot.send(state.context, Dialogues.devIdNotFound())
      id =
        waitTextMessageWithUser(state.context.id)
          .first()
          .content.text
          .toLongOrNull()
          ?.toAdminId()
    }

    return@strictlyOn MenuState(state.context)
  }
}
