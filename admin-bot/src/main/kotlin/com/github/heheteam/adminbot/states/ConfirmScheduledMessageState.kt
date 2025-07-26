package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.adminbot.timeFormatter
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
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
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ConfirmScheduledMessageState(
  override val context: User,
  val course: Course,
  val adminId: AdminId,
  val userGroup: UserGroup,
  val scheduledMessageTextField: ScheduledMessageTextField,
  val date: LocalDate,
  val time: LocalTime,
) : BotStateWithHandlers<Boolean, ScheduledMessageId?, AdminApi> {

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
    updateHandlersController: UpdateHandlersControllerDefault<Boolean>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val confirmationMessage =
      bot.send(
        context,
        buildEntities {
          +Dialogues.confirmScheduledMessage
          +"\n"
          bold("Тема:") + scheduledMessageTextField.shortDescription + "\n"
          bold("Текст:\n") + scheduledMessageTextField.content + "\n"
          bold("Время отправки: ") +
            time.format(timeFormatter) +
            " " +
            date.format(dateFormatter) +
            "\n"
          bold("Курс: ") + course.name + "\n"
        },
        replyMarkup = confirmationKeyboard(),
      )
    sentMessages.add(confirmationMessage)

    updateHandlersController.addDataCallbackHandler { callback ->
      when (callback.data) {
        "confirm" -> UserInput(true)
        "cancel" -> UserInput(false)
        else -> Unhandled
      }
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Boolean,
  ): Result<Pair<State, ScheduledMessageId?>, FrontendError> {
    return binding {
      if (input) {
        val scheduledMessage =
          service
            .sendScheduledMessage(
              adminId,
              LocalDateTime.of(date, time),
              TelegramMessageContent(scheduledMessageTextField.content),
              scheduledMessageTextField.shortDescription,
              userGroup,
            )
            .bind()
        Pair(MenuState(context, adminId), scheduledMessage)
      } else {
        Pair(MenuState(context, adminId), null)
      }
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: ScheduledMessageId?,
    input: Boolean,
  ): Result<Unit, FrontendError> =
    runCatching {
        val scheduledMessageId = response
        if (scheduledMessageId == null) {
          bot.send(context, "Операция отменена")
        } else {
          bot.send(context, "Ваше сообщение id=${scheduledMessageId.long} успешно отправлено!")
        }
        Unit
      }
      .toTelegramError()

  private fun confirmationKeyboard(): InlineKeyboardMarkup {
    return InlineKeyboardMarkup(
      keyboard =
        matrix {
          row { dataButton("Подтвердить", "confirm") }
          row { dataButton("Отмена", "cancel") }
        }
    )
  }
}
