package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

private const val TRUNCATED_MESSAGE_LENGTH = 50

data class DisplayRecentScheduledMessagesState(
  override val context: User,
  val adminId: AdminId,
  val courseId: CourseId,
  val numberOfMessages: Int,
  val fullTextConfirmed: Boolean,
) : BotStateWithHandlers<Unit, String, AdminApi> {

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<Unit>,
  ) {
    val messages =
      service.viewSentMessages(context.id, numberOfMessages).filter {
        it.courseId == courseId
      } // Filter by courseId
    // No sorting here, as per "sorted by the order from api call"

    val responseText =
      if (messages.isEmpty()) {
        buildEntities { +"Для выбранного курса нет запланированных сообщений." }
      } else {
        buildEntities {
          +"Последние запланированные сообщения для курса:\n\n"
          messages.forEach { message ->
            bold("ID: ${message.id.long}\n")
            bold("Время: ") + "${toReadableTimestampString(message.timestamp)}\n"
            bold("Короткое имя: ") + "${message.shortName}\n"
            bold("Администратор: ") +
              "${message.adminId.long}\n" // Displaying adminId as long for now
            bold("Содержание: ")
            +if (fullTextConfirmed) {
              message.content.text
            } else {
              message.content.text.truncate(TRUNCATED_MESSAGE_LENGTH)
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
            +"\n\n"
          }
        }
      }
    bot.sendMessage(context.id, responseText)
  }

  override fun computeNewState(service: AdminApi, input: Unit): Pair<State, String> {
    return MenuState(context, adminId) to "" // Return to menu after displaying
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: String) {
    // Response is already sent in intro, so this can be empty or send a final confirmation if
    // needed.
  }

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}

internal fun toReadableTimestampString(timestamp: LocalDateTime): String? =
  timestamp.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

private fun String.truncate(maxLength: Int): String {
  return if (this.length > maxLength) {
    this.substring(0, maxLength) + "..."
  } else {
    this
  }
}
