package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.teacherbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class WhoAmIState(override val context: User, val userId: TeacherId) :
  BotState<Unit, Unit, TeacherApi> {

  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherApi,
  ): Result<Unit, FrontendError> =
    runCatching {
        bot.send(context, Dialogues.sendTeacherId(userId))
        Unit
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: TeacherApi,
    input: Unit,
  ): Result<Pair<State, Unit>, FrontendError> {
    return (MenuState(context, userId) to Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
