package com.github.heheteam.adminbot.states.challenges

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.AdminKeyboards.RETURN_BACK
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
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

class QueryChallengeDescriptionState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private val assignmentId: AssignmentId,
) : SimpleState<AdminApi, AdminId>() {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()
  private var lastMessageId: MessageId? = null

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  override suspend fun BotContext.run(service: AdminApi) {
    val msg =
      bot
        .send(
          context,
          Dialogues.askAssignmentDescription,
          replyMarkup = AdminKeyboards.returnBack(),
        )
        .also { it.deleteLater() }
    lastMessageId = msg.messageId

    addDataCallbackHandler { callback ->
      if (callback.data == RETURN_BACK) {
        NewState(MenuState(context, userId))
      } else {
        Unhandled
      }
    }

    addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context, userId))
        else -> NewState(processDescriptionInput(message.content.text))
      }
    }
  }

  private fun processDescriptionInput(text: String): State {
    return if (text.contains("\$")) {
      val tokens = text.split("\$")
      if (tokens.size != 2) {
        CreateChallengeErrorState(
          context,
          courseId,
          assignmentId,
          "too many dollar signs in query",
          userId,
        )
      } else {
        val date = LocalDateTime.Formats.ISO.parseOrNull(tokens[1])
        QueryChallengeProblemDescriptionsState(
          context,
          userId,
          courseId,
          assignmentId,
          tokens[0] to date,
        )
      }
    } else {
      QueryChallengeProblemDescriptionsState(context, userId, courseId, assignmentId, text to null)
    }
  }
}
