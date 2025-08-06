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
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

class QueryChallengeProblemDescriptionsState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private val assignmentId: AssignmentId,
  private var description: Pair<String, LocalDateTime?>,
) : SimpleState<AdminApi, AdminId>() {

  private var lastMessageId: MessageId? = null

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    lastMessageId?.let {
      try {
        bot.delete(context.id, it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
    val msg =
      bot
        .send(context, Dialogues.askProblemsDescriptions, replyMarkup = AdminKeyboards.returnBack())
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
        "/menu" -> NewState(MenuState(context, userId))
        "/stop" -> NewState(MenuState(context, userId))
        else -> NewState(processProblemsInput(message.content.text))
      }
    }
  }

  private fun processProblemsInput(text: String): State {
    return parseProblemsDescriptions(text)
      .mapBoth(
        { result ->
          QueryChallengeStatementsUrlState(
            context,
            userId,
            courseId,
            assignmentId,
            description,
            result,
          )
        },
        { result -> CreateChallengeErrorState(context, courseId, assignmentId, result, userId) },
      )
  }
}
