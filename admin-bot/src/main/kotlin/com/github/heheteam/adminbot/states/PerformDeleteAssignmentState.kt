package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class PerformDeleteAssignmentState(
  override val context: User,
  val userId: AdminId,
  val assignmentId: AssignmentId,
) : BotState<Unit, Unit, AdminApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: AdminApi,
  ): Result<Unit, FrontendError> =
    runCatching {
        service
          .deleteAssignment(assignmentId)
          .mapBoth(
            success = { bot.send(context, "Серия успешно удалена!") },
            failure = { error ->
              bot.send(
                context,
                "Не удалось удалить серию. Пожалуйста, попробуйте еще раз.\n" +
                  "Ошибка №${error.number}: ${error.shortDescription}",
              )
            },
          )
        Unit
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: AdminApi,
    input: Unit,
  ): Result<Pair<State, Unit>, FrontendError> = (MenuState(context, userId) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
