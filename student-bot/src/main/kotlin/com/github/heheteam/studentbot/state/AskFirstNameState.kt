package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class AskFirstNameState(override val context: User, private val token: String?) :
  BotState<String, Unit, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): String {
    bot.sendSticker(context, Dialogues.greetingSticker)
    bot.send(context, Dialogues.greetings)
    bot.send(context, Dialogues.askFirstName)
    val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
    return firstName
  }

  override suspend fun computeNewState(service: StudentApi, input: String): Pair<State, Unit> {
    return AskLastNameState(context, input, token) to Unit
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit
}
