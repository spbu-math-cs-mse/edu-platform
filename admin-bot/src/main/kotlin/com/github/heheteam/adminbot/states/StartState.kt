package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotState
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User) : BotState<AdminId?, String?, AdminApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: AdminApi): AdminId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    TODO("Check if user is in whitelist")
    return service.loginByTgId(context.id).get()?.id
  }

  override fun computeNewState(service: AdminApi, input: AdminId?): Pair<State, String?> =
    if (input != null) {
      MenuState(context, input) to Dialogues.greetings
    } else {
      AskFirstNameState(context) to Dialogues.greetings
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: String?) {
    if (response != null) {
      bot.send(context, response)
    }
  }
}
