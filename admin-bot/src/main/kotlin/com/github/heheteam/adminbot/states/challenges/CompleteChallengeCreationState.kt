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
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row
import kotlinx.datetime.LocalDateTime

@Suppress("LongParameterList")
class CompleteChallengeCreationState(
  override val context: User,
  override val userId: AdminId,
  private val courseId: CourseId,
  private val assignmentId: AssignmentId,
  private var description: Pair<String, LocalDateTime?>,
  private var problems: List<ProblemDescription>,
  private var statementsUrl: String?,
) : SimpleState<AdminApi, AdminId>() {

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: AdminApi) {
    service
      .createChallenge(
        courseId,
        assignmentId,
        description.first,
        problems.map { it.copy(deadline = description.second) },
        statementsUrl,
      )
      .value
    send(
        Dialogues.challengeWasCreatedSuccessfully,
        inlineKeyboard { row { dataButton("Отлично!", AdminKeyboards.FICTITIOUS) } },
      )
      .deleteLater()
    addDataCallbackHandler { NewState(MenuState(context, userId)) }
  }
}
