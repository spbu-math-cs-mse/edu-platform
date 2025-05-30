package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

class CreateAssignmentErrorState(
  override val context: User,
  private val course: Course,
  private val errorMessage: String,
) : BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<State>,
  ) {
    val msg = bot.send(context, errorMessage, replyMarkup = AdminKeyboards.returnBack())
    sentMessages.add(msg)

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.RETURN_BACK) {
        NewState(CreateAssignmentState(context, course))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context))
        else -> Unhandled
      }
    }
  }

  override fun computeNewState(service: AdminApi, input: State) = Pair(input, Unit)

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: Unit) = Unit

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }
}
