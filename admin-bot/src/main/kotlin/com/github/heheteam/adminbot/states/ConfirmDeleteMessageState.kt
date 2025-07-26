package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createYesNoKeyboard
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities

data class ConfirmDeleteMessageState(
  override val context: User,
  val adminId: AdminId,
  val scheduledMessageId: ScheduledMessageId,
) : BotStateWithHandlers<Boolean, String, AdminApi> {

  private var confirmationMessage: AccessibleMessage? = null
  private var messageToConfirm: ScheduledMessage? = null

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<Boolean>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val result = service.resolveScheduledMessage(scheduledMessageId)
    result.mapBoth(
      success = { message ->
        messageToConfirm = message
        val formattedMessage = formatScheduleMessage(message)

        val keyboard = createYesNoKeyboard("Да", "Нет")
        confirmationMessage =
          bot.sendMessage(context.id, formattedMessage, replyMarkup = keyboard.keyboard)
        updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
          keyboard
            .handler(dataCallbackQuery.data)
            .mapBoth(
              success = { UserInput(it) },
              failure = { Unhandled }, // Should not happen with Yes/No
            )
        }
      },
      failure = { error ->
        bot.sendMessage(
          context.id,
          "Не удалось найти сообщение с ID ${scheduledMessageId.long} или оно уже удалено.",
        )
        computeNewState(service, false)
      },
    )
  }

  private fun formatScheduleMessage(message: ScheduledMessage): TextSourcesList = buildEntities {
    bold("Вы собираетесь удалить следующее сообщение:\n\n")
    bold("ID: ${message.id.long}\n")
    bold("Время: ${toReadableTimestampString(message.timestamp)}\n")
    bold("Короткое имя: ${message.shortName}\n")
    bold("Администратор: ${message.adminId.long}\n")
    bold("Содержание: ${message.content.text}\n")
    bold("Статус: ")
    +if (message.isDeleted) {
      "Удалено"
    } else if (message.isSent) {
      "Отправлено"
    } else {
      "Ожидает отправки"
    }
    +"\n\n"
    +"Вы уверены, что хотите удалить это сообщение?"
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Boolean,
  ): Result<Pair<State, String>, FrontendError> {
    return if (input) {
        PerformDeleteMessageState(context, adminId, scheduledMessageId) to "Удаление сообщения..."
      } else {
        MenuState(context, adminId) to "Удаление сообщения отменено."
      }
      .ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String,
    input: Boolean,
  ): Result<Unit, FrontendError> =
    runCatching {
        confirmationMessage?.let { bot.delete(it) }
        if (response.isNotEmpty()) {
          bot.sendMessage(context.id, response)
        }
        Unit
      }
      .toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
