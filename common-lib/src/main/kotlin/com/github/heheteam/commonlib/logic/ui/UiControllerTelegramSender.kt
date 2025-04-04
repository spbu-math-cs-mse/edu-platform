package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error

internal class UiControllerTelegramSender(
  private val studentNotifier: StudentNewGradeNotifier,
  private val journalUpdater: JournalUpdater,
  private val menuMessageUpdater: MenuMessageUpdater,
  private val solutionDistributor: SolutionDistributor,
) : UiController {
  override fun updateUiOnSolutionAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    studentNotifier.notifyStudentOnNewAssessment(solutionId, assessment)
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
    val teacherId = solutionDistributor.resolveSolution(solutionId).get()?.responsibleTeacherId
    if (teacherId != null) {
      menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId).onFailure { KSLog.error(it) }
    }

    val courseId = solutionDistributor.resolveSolutionCourse(solutionId).get()
    if (courseId != null) {
      menuMessageUpdater.updateMenuMessageInGroupChat(courseId).onFailure { KSLog.error(it) }
    }
  }
}
