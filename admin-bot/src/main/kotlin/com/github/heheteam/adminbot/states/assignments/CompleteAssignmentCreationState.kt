package com.github.heheteam.adminbot.states.assignments

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.challenges.QueryChallengeDescriptionState
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

class CompleteAssignmentCreationState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private var description: Pair<String, LocalDateTime?>,
  private var problems: List<ProblemDescription>,
  private var statementsUrl: String,
) : SimpleState<AdminApi, AdminId>() {

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    val assignmentId =
      service
        .createAssignment(
          courseId,
          description.first,
          problems.map { it.copy(deadline = description.second) },
          if (statementsUrl == "") null else statementsUrl,
        )
        .value
    send(Dialogues.assignmentWasCreatedSuccessfully, AdminKeyboards.yesNo()).deleteLater()
    addDataCallbackHandler { callback ->
      when (callback.data) {
        AdminKeyboards.YES ->
          NewState(QueryChallengeDescriptionState(context, userId, courseId, assignmentId))
        AdminKeyboards.NO -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
