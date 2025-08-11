package com.github.heheteam.adminbot.states.challenges

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

class CreateChallengeErrorState(
  override val context: User,
  private val courseId: CourseId,
  private val assignmentId: AssignmentId,
  private val errorMessage: String,
  override val userId: AdminId,
) : SimpleState<AdminApi, AdminId>() {

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    send(errorMessage, replyMarkup = AdminKeyboards.returnBack()).deleteLater()

    addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.RETURN_BACK) {
        NewState(QueryChallengeDescriptionState(context, userId, courseId, assignmentId))
      } else {
        Unhandled
      }
    }

    addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
