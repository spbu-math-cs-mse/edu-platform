package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class AddScheduledMessageStartState(
  override val context: User,
  val course: Course,
  val adminId: AdminId,
) : BotStateWithHandlers<State, Unit, AdminApi> {

  val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<State>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val introMessage = bot.send(context, Dialogues.addScheduledMessageStartSummary)
    sentMessages.add(introMessage)
  }

  override suspend fun computeNewState(service: AdminApi, input: State): Pair<State, Unit> {
    return Pair(QueryScheduledMessageContentState(context, course, adminId), Unit)
  }

  override suspend fun handle(
    bot: BehaviourContext,
    service: AdminApi,
    initUpdateHandlers:
      (UpdateHandlersController<SuspendableBotAction, State, EduPlatformError>, User) -> Unit,
  ): State {
    return QueryScheduledMessageContentState(context, course, adminId)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: State,
  ) = Unit

  override fun defaultState(): State = MenuState(context, adminId)
}
