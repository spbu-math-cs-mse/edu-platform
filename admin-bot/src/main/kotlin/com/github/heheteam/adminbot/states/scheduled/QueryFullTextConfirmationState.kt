package com.github.heheteam.adminbot.states.scheduled

import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createYesNoKeyboard
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.EntitiesBuilder
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.extensions.makeString
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

data class QueryFullTextConfirmationState(
  override val context: User,
  val adminId: AdminId,
  val courseId: CourseId? = null,
  val numberOfMessages: Int,
) : BotStateWithHandlers<Boolean, Result<List<ScheduledMessage>, FrontendError>, AdminApi> {

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<Boolean>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val keyboard = createYesNoKeyboard("Да", "Нет")
    bot.sendMessage(
      context.id,
      buildEntities { +"Показать полный текст сообщений?" },
      replyMarkup = keyboard.keyboard,
    )
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      keyboard
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: Boolean,
  ): Result<Pair<State, Result<List<ScheduledMessage>, FrontendError>>, FrontendError> {
    val messages = service.viewScheduledMessages(null, null, numberOfMessages)
    return (MenuState(context, adminId) to messages).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Result<List<ScheduledMessage>, FrontendError>,
    input: Boolean,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val messages = response.bind()
    val responseText =
      if (messages.isEmpty()) {
        buildEntities { +"Для выбранного курса нет запланированных сообщений." }
      } else {
        buildEntities {
          +"Последние запланированные сообщения для курса:\n\n"
          messages.forEach { message ->
            formatSingleMessage(message, input)
            +"\n\n"
          }
        }
      }
    bot.send(context.id, responseText)
  }

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit

  private fun EntitiesBuilder.formatSingleMessage(
    message: ScheduledMessage,
    fullTextConfirmed: Boolean,
  ) {
    bold("ID: ${message.id.long}\n")
    bold("Время: ") + "${toReadableTimestampString(message.timestamp)}\n"
    bold("Группа: ") + "${message.userGroup.toString()}\n"
    bold("Тема: ") + "${message.shortName}\n"
    bold("Администратор: id=") + "${message.adminId.long}\n"
    bold("Содержание: ")
    if (fullTextConfirmed) {
      +message.content.text
    } else {
      +message.content.text.makeString().truncate(TRUNCATED_MESSAGE_LENGTH)
    }
    +"\n"
    bold("Статус: ")
    +if (message.isDeleted) {
      "Удалено"
    } else if (message.isSent) {
      "Отправлено"
    } else {
      "Ожидает отправки"
    }
  }

  private fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
      this.substring(0, maxLength) + "..."
    } else {
      this
    }
  }
}

internal const val TRUNCATED_MESSAGE_LENGTH = 50

internal fun toReadableTimestampString(timestamp: LocalDateTime): String? =
  timestamp.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
