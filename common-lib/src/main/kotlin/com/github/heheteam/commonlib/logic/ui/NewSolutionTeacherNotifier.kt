package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error

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
      menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId).onFailure { KSLog.error(it) }
    }
    val courseId = solutionCourseResolver.resolveCourse(solution.id).get()
    if (courseId != null) {
      menuMessageUpdater.updateMenuMessageInGroupChat(courseId).onFailure { KSLog.error(it) }
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
          .bind() ?: return@binding
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
