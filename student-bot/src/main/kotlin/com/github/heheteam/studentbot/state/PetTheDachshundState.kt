package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class PetTheDachshundState(override val context: User, val userId: StudentId) :
  BotState<Unit, Unit, StudentApi> {

  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi) {
    bot.sendMessage(context.id, listOf("Гаф", "Мяу").random())
  }

  override suspend fun computeNewState(service: StudentApi, input: Unit): Pair<State, Unit> =
    MenuState(context, userId) to Unit

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit
}
