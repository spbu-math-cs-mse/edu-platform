package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class AskFirstNameState(override val context: User) : BotState<String, Unit, AdminApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: AdminApi): String {
    bot.send(context, Dialogues.askFirstName)
    val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
    return firstName
  }

  override fun computeNewState(service: AdminApi, input: String): Pair<State, Unit> {
    return AskLastNameState(context, input) to Unit
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: Unit) = Unit
}
