package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

class NewSolutionTeacherNotifier(
  private val telegramSolutionSender: TelegramSolutionSender,
  private val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val solutionCourseResolver: SolutionCourseResolver,
  private val menuMessageUpdater: MenuMessageUpdater,
) {
  fun notifyNewSolution(solution: Solution): Result<Unit, SolutionSendingError> = binding {
    sendSolutionToTeacherPersonally(solution)
    sendSolutionToGroup(solution)
    val teacherId = solution.responsibleTeacherId
    if (teacherId != null) {
      menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId)
    }
  }

  private fun sendSolutionToGroup(solution: Solution): Result<Unit, SolutionSendingError> =
    binding {
      val courseOfSolution =
        solutionCourseResolver
          .resolveCourse(solution.id)
          .mapError { NoCourseFoundFor(solution.id) }
          .bind()
      val groupTechnicalMessage =
        telegramSolutionSender
          .sendGroupSolutionNotification(courseOfSolution, solution)
          .mapError { SendToGroupSolutionError(courseOfSolution) }
          .bind()
      telegramTechnicalMessageStorage.registerGroupSolutionPublication(
        solution.id,
        groupTechnicalMessage,
      )
    }

  private fun sendSolutionToTeacherPersonally(
    solution: Solution
  ): Result<Unit, SolutionSendingError> = binding {
    val teacherId =
      solution.responsibleTeacherId.toResultOr { NoResponsibleTeacherFor(solution) }.bind()

    val personalTechnicalMessage =
      telegramSolutionSender
        .sendPersonalSolutionNotification(teacherId, solution)
        .mapError { SendToTeacherSolutionError(teacherId) }
        .bind()
    telegramTechnicalMessageStorage.registerPersonalSolutionPublication(
      solution.id,
      personalTechnicalMessage,
    )
  }
}
