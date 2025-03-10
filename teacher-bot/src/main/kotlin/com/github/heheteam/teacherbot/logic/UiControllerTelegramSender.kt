package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId

class UiControllerTelegramSender(
  val studentNotifier: StudentNewGradeNotifier,
  val journalUpdater: JournalUpdater,
) : UiController {
  override fun updateUiOnSolutionAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    studentNotifier.notifyStudentOnNewAssignment(solutionId, assessment)
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
  }
}
