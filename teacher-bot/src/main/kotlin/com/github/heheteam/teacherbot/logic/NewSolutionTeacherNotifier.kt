package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.table.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

class NewSolutionTeacherNotifier(
  val teacherStorage: TeacherStorage,
  val telegramSolutionSender: TelegramSolutionSender,
  val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
) {
  fun notifyNewSolution(solution: Solution): Result<Unit, SolutionSendingError> = binding {
    val teacherId =
      solution.responsibleTeacherId.toResultOr { NoResponsibleTeacherFor(solution) }.bind()
    val teacher =
      teacherStorage.resolveTeacher(teacherId).mapError { NoTeacherResolvedFor(teacherId) }.bind()
    val sendPersonalSolutionNotification =
      telegramSolutionSender.sendPersonalSolutionNotification(teacher.tgId, solution)
    val personalTechnicalMessage =
      sendPersonalSolutionNotification.mapError { UninitializedTeacherBot }.bind()
    telegramTechnicalMessageStorage.registerPersonalSolutionPublication(
      solution.id,
      personalTechnicalMessage,
    )
  }
}
