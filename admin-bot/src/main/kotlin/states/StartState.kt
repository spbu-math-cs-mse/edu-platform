package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminId
import com.github.heheteam.commonlib.api.AdminIdRegistry
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(userIdRegistry: AdminIdRegistry, isDeveloperRun: Boolean = false) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    if (!isDeveloperRun) {
      var id: AdminId? = userIdRegistry.getUserId(state.context.id)
      if (id != null) {
        return@strictlyOn MenuState(state.context)
      }
    } else {
      bot.send(state.context, Dialogues.devAskForId())
      while (true) {
        val id = waitTextMessage().first().content.text.toLongOrNull()?.let { userIdRegistry.getUserId(UserId(RawChatId(it))) }
        if (id == null) {
          bot.send(state.context, Dialogues.devIdNotFound())
          continue
        }
        break
      }
    }

    bot.send(state.context, Dialogues.greetings())
    return@strictlyOn MenuState(state.context)
  }
}
