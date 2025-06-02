package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createYesNoKeyboard
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
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

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController:
      UpdateHandlersController<BehaviourContext.() -> Unit, Boolean, EduPlatformError>,
  ) {
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
        // Transition to MenuState immediately if message not found
        computeNewState(service, false) // Simulate 'No' to go to MenuState
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

  override fun computeNewState(service: AdminApi, input: Boolean): Pair<State, String> {
    return if (input) {
      PerformDeleteMessageState(context, adminId, scheduledMessageId) to "Удаление сообщения..."
    } else {
      MenuState(context, adminId) to "Удаление сообщения отменено."
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String,
    input: Boolean,
  ) {
    confirmationMessage?.let {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete confirmation message", e)
      }
    }
    if (response.isNotEmpty()) {
      bot.sendMessage(context.id, response)
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
