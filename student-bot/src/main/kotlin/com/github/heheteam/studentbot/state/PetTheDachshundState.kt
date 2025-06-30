package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class PetTheDachshundState(override val context: User, val userId: StudentId) :
  BotState<Unit, Unit, StudentApi> {

  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: StudentApi,
  ): Result<Unit, FrontendError> =
    runCatching {
        bot.sendMessage(context.id, listOf("Гаф", "Мяу").random())
        Unit
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: StudentApi,
    input: Unit,
  ): Result<Pair<State, Unit>, FrontendError> = (MenuState(context, userId) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
