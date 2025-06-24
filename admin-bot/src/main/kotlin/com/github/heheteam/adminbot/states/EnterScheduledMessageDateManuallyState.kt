package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.OperationCancelledError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
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
import java.time.LocalDate
import java.time.format.DateTimeParseException

class EnterScheduledMessageDateManuallyState(
  override val context: User,
  val course: Course,
  val adminId: AdminId,
  val scheduledMessageTextField: ScheduledMessageTextField,
  val error: EduPlatformError? = null,
) : BotStateWithHandlers<Result<LocalDate, EduPlatformError>, EduPlatformError?, AdminApi> {

  val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, adminId)

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
    updateHandlersController: UpdateHandlerManager<Result<LocalDate, EduPlatformError>>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    val introMessage = bot.send(context, Dialogues.enterScheduledMessageDateManually)
    sentMessages.add(introMessage)

    error?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }

    updateHandlersController.addTextMessageHandler { message ->
      val text = message.content.text
      if (text == "/stop") {
        UserInput(Err(OperationCancelledError()))
      } else {
        try {
          UserInput(Ok(LocalDate.parse(text, dateFormatter)))
        } catch (_: DateTimeParseException) {
          UserInput(Err(NamedError(Dialogues.invalidDateFormat)))
        }
      }
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Result<LocalDate, EduPlatformError>,
  ): Pair<State, EduPlatformError?> {
    return input.mapBoth(
      success = { date ->
        Pair(
          QueryScheduledMessageTimeState(context, course, adminId, scheduledMessageTextField, date),
          null,
        )
      },
      failure = { error ->
        if (error is OperationCancelledError) {
          Pair(MenuState(context, adminId), null)
        } else {
          Pair(this, error)
        }
      },
    )
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: EduPlatformError?,
    input: Result<LocalDate, EduPlatformError>,
  ) {
    response?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }
  }
}
