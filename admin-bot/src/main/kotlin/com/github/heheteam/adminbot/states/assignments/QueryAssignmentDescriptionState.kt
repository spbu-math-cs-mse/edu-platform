package com.github.heheteam.adminbot.states.assignments

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.AdminKeyboards.RETURN_BACK
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.SimpleAdminState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

class QueryAssignmentDescriptionState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
) : SimpleAdminState() {

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    send(Dialogues.askAssignmentDescription, replyMarkup = AdminKeyboards.returnBack())
      .deleteLater()

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
        CreateAssignmentErrorState(context, courseId, "too many dollar signs in query", userId)
      } else {
        val date = LocalDateTime.Formats.ISO.parseOrNull(tokens[1])
        QueryProblemDescriptionsState(context, userId, courseId, tokens[0] to date)
      }
    } else {
      QueryProblemDescriptionsState(context, userId, courseId, text to null)
    }
  }
}
