package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.toNumberedResult
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities

private const val MAXIMUM_SCHEDULED_MSGS_DISPLAYED = 100

data class QueryNumberOfRecentMessagesState(
  override val context: User,
  val adminId: AdminId,
  val courseId: CourseId,
) : BotStateWithHandlers<String, String, AdminApi> {

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    bot.sendMessage(
      context.id,
      buildEntities {
        +"Введите количество последних запланированных сообщений для отображения (рекомендуется "
        bold("5")
        +"): "
      },
    )
    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, String>, NumberedError> {
    val number = input.toIntOrNull()
    return if (number == null || number <= 0 || number > MAXIMUM_SCHEDULED_MSGS_DISPLAYED) {
        QueryNumberOfRecentMessagesState(context, adminId, courseId) to
          "Пожалуйста, введите число от 1 до 100."
      } else {
        QueryFullTextConfirmationState(context, adminId, courseId, number) to
          "Загрузка сообщений..."
      }
      .ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String,
    input: String,
  ): Result<Unit, NumberedError> =
    runCatching {
        bot.sendMessage(context.id, response)
        Unit
      }
      .toNumberedResult()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
