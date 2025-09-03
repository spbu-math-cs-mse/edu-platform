package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.commonlib.AssignmentDependencies
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.boldln
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regularln

data class ConfirmDeleteAssignmentState(
  override val context: User,
  override val userId: AdminId,
  val assignmentId: AssignmentId,
) : SimpleState<AdminApi, AdminId>() {

  private fun formatAssignment(assignmentDependencies: AssignmentDependencies): TextSourcesList =
    buildEntities {
      bold("Вы собираетесь удалить серию:${assignmentDependencies.assignment.description}\n")
      bold("ID: ${assignmentDependencies.assignment.id.long}\n\n")
      boldln("Вместе с ней удалятся:")
      bold("${assignmentDependencies.numberOfProblems}")
      regularln(" задач")
      bold("${assignmentDependencies.numberOfSubmissions}")
      regularln(" посылок учеников")
      bold("${assignmentDependencies.numberOfAssessments}")
      regularln(" проверок преподавателей")
      +"\n\n"
      +"Вы уверены, что хотите удалить эту серию?"
    }

  override suspend fun BotContext.run(service: AdminApi) {
    val assignmentDependencies = service.resolveAssignmentAndDependencies(assignmentId).value
    send(formatAssignment(assignmentDependencies), AdminKeyboards.yesNo()).deleteLater()
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        AdminKeyboards.YES -> NewState(PerformDeleteAssignmentState(context, userId, assignmentId))
        AdminKeyboards.NO -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }

  override fun defaultState(): State = MenuState(context, userId)
}
