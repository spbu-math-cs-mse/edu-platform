package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.get

class UiControllerTelegramSender(
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
      menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId)
    }
  }
}
