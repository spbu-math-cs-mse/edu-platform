package com.github.heheteam.adminbot.states.assignments

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.SimpleAdminState
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

@Suppress("")
class QueryStatementsUrlState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private var description: Pair<String, LocalDateTime?>,
  private var problems: List<ProblemDescription>,
) : SimpleAdminState() {

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    send(Dialogues.askStatementsUrl, replyMarkup = AdminKeyboards.skipThisStep()).deleteLater()

    addDataCallbackHandler { callback ->
      if (callback.data == AdminKeyboards.SKIP_THIS_STEP) {
        NewState(
          CompleteAssignmentCreationState(context, userId, courseId, description, problems, "")
        )
      } else {
        Unhandled
      }
    }

    addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context, userId))
        else ->
          if (message.content.text.isUrl()) {
            NewState(
              CompleteAssignmentCreationState(
                context,
                userId,
                courseId,
                description,
                problems,
                message.content.text,
              )
            )
          } else {
            send(Dialogues.invalidUrlFormat)
            Unhandled
          }
      }
    }
  }

  private fun String.isUrl(): Boolean = startsWith("http://") || startsWith("https://")
}
