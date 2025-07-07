package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class WhoAmIState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<Unit, Unit, StudentApi> {
  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Unit, FrontendError>,
  ): Result<Unit, FrontendError> =
    runCatching {
        bot.send(context, Dialogues.sendStudentId(userId))
        Unit
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: StudentApi,
    input: Unit,
  ): Result<Pair<State, Unit>, FrontendError> {
    return (MenuState(context, userId) to Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
