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
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

class QueryProblemDescriptionsState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private var description: Pair<String, LocalDateTime?>,
) : SimpleAdminState() {

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    send(Dialogues.askProblemsDescriptions, replyMarkup = AdminKeyboards.returnBack()).deleteLater()

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
        { result -> QueryStatementsUrlState(context, userId, courseId, description, result) },
        { result -> CreateAssignmentErrorState(context, courseId, result, userId) },
      )
  }
}
