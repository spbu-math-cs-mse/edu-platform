package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.NamedError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createYesNoKeyboard
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

data class QueryFullTextConfirmationState(
  override val context: User,
  val adminId: AdminId,
  val courseId: CourseId,
  val numberOfMessages: Int,
) : BotStateWithHandlers<Boolean, String, AdminApi> {

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<Boolean>,
  ) {
    val keyboard = createYesNoKeyboard("Да", "Нет")
    bot.sendMessage(
      context.id,
      buildEntities { +"Показать полный текст сообщений?" },
      replyMarkup = keyboard.keyboard,
    )
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      keyboard
        .handler(dataCallbackQuery.data)
        .mapBoth(
          success = { UserInput(it) },
          failure = { HandlingError(NamedError("Invalid input for Yes/No confirmation")) },
        )
    }
  }

  override fun computeNewState(service: AdminApi, input: Boolean): Pair<State, String> {
    return DisplayRecentScheduledMessagesState(
      context,
      adminId,
      courseId,
      numberOfMessages,
      input,
    ) to "Подготовка сообщений..."
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: String) {
    bot.sendMessage(context.id, response)
  }

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
