package com.github.heheteam.adminbot.states.challenges

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

@Suppress("")
class QueryChallengeStatementsUrlState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private val assignmentId: AssignmentId,
  private var description: Pair<String, LocalDateTime?>,
  private var problems: List<ProblemDescription>,
) : SimpleState<AdminApi, AdminId>() {

  private var lastMessageId: MessageId? = null

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    val msg =
      bot
        .send(context, Dialogues.askStatementsUrl, replyMarkup = AdminKeyboards.skipThisStep())
        .also { it.deleteLater() }
    lastMessageId = msg.messageId

    addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.SKIP_THIS_STEP) {
        newState()
      } else {
        Unhandled
      }
    }

    addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context, userId))
        else ->
          if (message.content.text.isUrl()) {
            newState(message.content.text)
          } else {
            bot.send(context, Dialogues.invalidUrlFormat)
            Unhandled
          }
      }
    }
  }

  private fun newState(statementsUrl: String? = null): NewState =
    NewState(
      CompleteChallengeCreationState(
        context,
        userId,
        courseId,
        assignmentId,
        description,
        problems,
        statementsUrl,
      )
    )

  private fun String.isUrl(): Boolean = startsWith("http://") || startsWith("https://")
}
