package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId

class UiControllerTelegramSender(
  val studentNotifier: StudentNewGradeNotifier,
  val journalUpdater: JournalUpdater,
  val menuMessageUpdater: MenuMessageUpdater,
) : UiController {
  override fun updateUiOnSolutionAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    studentNotifier.notifyStudentOnNewAssessment(solutionId, assessment)
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
    menuMessageUpdater.updateMenuMessageInPersonalChat(solutionId)
  }
}
