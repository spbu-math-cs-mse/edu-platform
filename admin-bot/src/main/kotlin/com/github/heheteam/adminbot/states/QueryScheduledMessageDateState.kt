package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.adminbot.toRussian
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.NamedError
import com.github.heheteam.commonlib.OperationCancelledError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UserInput
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Suppress("MagicNumber") // working with dates
class QueryScheduledMessageDateState(
  override val context: User,
  val course: Course,
  val adminId: AdminId,
  val scheduledMessageTextField: ScheduledMessageTextField,
  val error: EduPlatformError? = null,
) : BotStateWithHandlers<Result<LocalDate, EduPlatformError>, EduPlatformError?, AdminApi> {

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
    updateHandlersController: UpdateHandlerManager<Result<LocalDate, EduPlatformError>>,
  ) {
    val introMessage =
      bot.send(context, Dialogues.queryScheduledMessageDate, replyMarkup = keyboardWithDates())
    sentMessages.add(introMessage)

    error?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }

    updateHandlersController.addDataCallbackHandler { callback -> handleKeyboardCallback(callback) }

    updateHandlersController.addTextMessageHandler { message -> handleMessageCallback(message) }
  }

  private fun handleMessageCallback(
    message: CommonMessage<TextContent>
  ): HandlerResultWithUserInputOrUnhandled<
    BehaviourContext.() -> Unit,
    Result<LocalDate, EduPlatformError>,
    EduPlatformError,
  > {
    val text = message.content.text
    return if (text == "/stop") {
      UserInput(Err(OperationCancelledError()))
    } else {
      try {
        UserInput(Ok(LocalDate.parse(text, dateFormatter)))
      } catch (_: DateTimeParseException) {
        UserInput(Err(NamedError(Dialogues.invalidDateFormat)))
      }
    }
  }

  private fun handleKeyboardCallback(
    callback: DataCallbackQuery
  ): HandlerResultWithUserInputOrUnhandled<
    BehaviourContext.() -> Unit,
    Result<LocalDate, EduPlatformError>,
    EduPlatformError,
  > =
    when (callback.data) {
      "enter date" -> {
        NewState(
          EnterScheduledMessageDateManuallyState(
            context,
            course,
            adminId,
            scheduledMessageTextField,
          )
        )
      }

      "cancel" -> UserInput(Err(OperationCancelledError()))
      else -> {
        try {
          UserInput(Ok(LocalDate.parse(callback.data, dateFormatter)))
        } catch (_: DateTimeParseException) {
          UserInput(Err(NamedError(Dialogues.invalidDateButtonFormat)))
        }
      }
    }

  override fun computeNewState(
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
  ) {
    response?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }
  }

  private fun keyboardWithDates(): InlineKeyboardMarkup {
    val today = LocalDate.now()
    val dates =
      listOf(
        today,
        today.plusDays(1),
        today.plusDays(2),
        today.plusDays(3),
        today.plusDays(4),
        today.plusDays(5),
        today.plusDays(6),
      )
    return InlineKeyboardMarkup(
      keyboard =
        matrix {
          row {
            dataButton(
              dates[0].format(dateFormatter) + " (сегодня)",
              dates[0].format(dateFormatter),
            )
          }
          row {
            dataButton(dates[1].format(dateFormatter) + " (завтра)", dates[1].format(dateFormatter))
          }
          (2..6).map {
            row {
              dataButton(
                dates[it].format(dateFormatter) + " (" + toRussian(dates[it].dayOfWeek) + ")",
                dates[it].format(dateFormatter),
              )
            }
          }
          row { dataButton("Ввести с клавиатуры", "enter date") }
          row { dataButton("Отмена", "cancel") }
        }
    )
  }
}
