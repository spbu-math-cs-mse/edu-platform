package com.github.heheteam.adminbot.states.scheduled

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.extractTextWithMediaAttachments
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.utils.extensions.makeString

class QueryScheduledMessageContentState(
  override val context: User,
  val adminId: AdminId,
  val userGroup: UserGroup,
  val error: EduPlatformError? = null,
) : BotStateWithHandlers<ScheduledMessageContentField?, Unit, AdminApi> {
  lateinit var adminBotToken: String
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

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<ScheduledMessageContentField?>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val introMessage = bot.send(context, Dialogues.queryScheduledMessageContent)
    sentMessages.add(introMessage)

    error?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }

    updateHandlersController.addTextMessageHandler { message ->
      bot.parseSentSubmission(message, adminBotToken)
    }
    updateHandlersController.addMediaMessageHandler { message ->
      bot.setMessageReaction(message, "\uD83E\uDD23")
      bot.parseSentSubmission(message, adminBotToken)
    }
    updateHandlersController.addDocumentMessageHandler { message ->
      bot.parseSentSubmission(message, adminBotToken)
    }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: ScheduledMessageContentField?,
  ): Result<Pair<State, Unit>, FrontendError> {
    return if (input != null) {
        QueryScheduledMessageDateState(context, adminId, userGroup, input) to Unit
      } else {
        QueryScheduledMessageContentState(context, adminId, userGroup, error) to Unit
      }
      .ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: ScheduledMessageContentField?,
  ): Result<Unit, FrontendError> = Unit.ok()

  private suspend fun BehaviourContext.parseSentSubmission(
    submissionMessage: CommonMessage<*>,
    adminBotToken: String,
  ): HandlerResultWithUserInputOrUnhandled<Nothing, ScheduledMessageContentField?, FrontendError> {
    val content =
      extractTextWithMediaAttachments(submissionMessage, adminBotToken, this)
        ?: return UserInput(null)
    val shortDescription = content.text.makeString().lines().firstOrNull().orEmpty()
    return UserInput(ScheduledMessageContentField(shortDescription, content))
  }
}
