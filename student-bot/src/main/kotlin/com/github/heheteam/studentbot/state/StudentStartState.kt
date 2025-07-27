package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StudentStartState(
  override val context: User,
  private val token: String?,
  private val from: String? = null,
) : BotState<StudentId?, Unit, StudentApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: StudentApi,
  ): Result<StudentId?, FrontendError> = coroutineBinding {
    runCatching {
        val id = service.loginByTgId(context.id).bind()?.id
        return@runCatching id
      }
      .toTelegramError()
      .bind()
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: StudentId?,
  ): Result<Pair<State, Unit>, FrontendError> =
    if (input != null) {
        MenuState(context, input) to Unit
      } else {
        AskStudentFirstNameState(context, token, from) to Unit
      }
      .ok()

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit.ok()
}
