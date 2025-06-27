package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.newStateError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.utils.buildEntities

class QueryScheduledMessageContentState(
  override val context: User,
  val course: Course,
  val adminId: AdminId,
  val error: EduPlatformError? = null,
) : BotStateWithHandlers<Result<ScheduledMessageTextField, EduPlatformError>, Unit, AdminApi> {

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

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController:
      UpdateHandlerManager<Result<ScheduledMessageTextField, EduPlatformError>>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    val introMessage = bot.send(context, Dialogues.queryScheduledMessageContent)
    sentMessages.add(introMessage)

    error?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }

    updateHandlersController.addTextMessageHandler { message ->
      val text = message.content.text
      if (text.isBlank()) {
        UserInput(Err(newStateError(Dialogues.scheduledMessageContentEmptyError)))
      } else {
        val lines = text.lines()
        val shortDescription = lines.first()
        val content = buildEntities { +lines.drop(1).joinToString("\n") }
        UserInput(Ok(ScheduledMessageTextField(shortDescription, content)))
      }
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Result<ScheduledMessageTextField, EduPlatformError>,
  ): Result<Pair<State, Unit>, NumberedError> {
    return input
      .mapBoth(
        success = { scheduledMessageTextField ->
          Pair(
            QueryScheduledMessageDateState(context, course, adminId, scheduledMessageTextField),
            Unit,
          )
        },
        failure = { error ->
          Pair(QueryScheduledMessageContentState(context, course, adminId, error), Unit)
        },
      )
      .ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: Result<ScheduledMessageTextField, EduPlatformError>,
  ): Result<Unit, NumberedError> = Unit.ok()
}
