package com.github.heheteam.adminbot.states.assignments

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.AdminKeyboards.RETURN_BACK
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.datetime.LocalDateTime

class QueryAssignmentDescriptionState(
  override val context: User,
  val adminId: AdminId,
  private val course: Course,
) : BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()
  private var lastMessageId: MessageId? = null

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
    updateHandlersController: UpdateHandlersControllerDefault<State>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val msg =
      bot.send(
        context,
        Dialogues.askAssignmentDescription,
        replyMarkup = AdminKeyboards.returnBack(),
      )
    sentMessages.add(msg)
    lastMessageId = msg.messageId

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == RETURN_BACK) {
        NewState(MenuState(context, adminId))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context, adminId))
        else -> NewState(processDescriptionInput(message.content.text))
      }
    }
  }

  private fun processDescriptionInput(text: String): State {
    return if (text.contains("\$")) {
      val tokens = text.split("\$")
      if (tokens.size != 2) {
        CreateAssignmentErrorState(context, course, "too many dollar signs in query", adminId)
      } else {
        val date = LocalDateTime.Formats.ISO.parseOrNull(tokens[1])
        QueryProblemDescriptionsState(context, adminId, course, tokens[0] to date)
      }
    } else {
      QueryProblemDescriptionsState(context, adminId, course, text to null)
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> = Pair(input, Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: State,
  ): Result<Unit, FrontendError> = Unit.ok()
}
