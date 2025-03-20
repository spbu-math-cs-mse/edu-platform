package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.database.table.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning

class NewSolutionTeacherNotifier(
  private val telegramSolutionSender: TelegramSolutionSender,
  private val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val solutionCourseResolver: SolutionCourseResolver,
) {
  fun notifyNewSolution(solution: Solution): Result<Unit, SolutionSendingError> = binding {
    sendSolutionToTeacherPersonally(solution)
    sendSolutionToGroup(solution)
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
      solution.responsibleTeacherId
        .toResultOr {
          KSLog.warning("No responsible teacher for solution: $solution")
          NoResponsibleTeacherFor(solution)
        }
        .bind()

    val personalTechnicalMessage =
      telegramSolutionSender
        .sendPersonalSolutionNotification(teacherId, solution)
        .mapError {
          KSLog.warning("Failed to send solution $solution to the teacher with id: $teacherId")
          SendToTeacherSolutionError(teacherId)
        }
        .bind()
    telegramTechnicalMessageStorage.registerPersonalSolutionPublication(
      solution.id,
      personalTechnicalMessage,
    )
  }
}
