package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.state.parent.ParentMenuState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class ParentStartState(override val context: User, private val from: String?) :
  BotState<ParentId?, Unit, ParentApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: ParentApi,
  ): Result<ParentId?, FrontendError> = coroutineBinding {
    val id = service.tryLoginByTelegramId(context.id.chatId).bind()?.id
    id
  }

  override suspend fun computeNewState(
    service: ParentApi,
    input: ParentId?,
  ): Result<Pair<State, Unit>, FrontendError> =
    if (input != null) {
        ParentMenuState(context, input) to Unit
      } else {
        AskParentFirstNameState(context, from) to Unit
      }
      .ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ParentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
