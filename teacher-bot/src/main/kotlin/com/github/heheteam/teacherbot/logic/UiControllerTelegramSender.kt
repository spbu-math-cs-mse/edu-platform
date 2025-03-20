package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UiControllerTelegramSender : UiController, KoinComponent {
  private val studentNotifier: StudentNewGradeNotifier by inject()
  private val journalUpdater: JournalUpdater by inject()

  override fun updateUiOnSolutionAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    studentNotifier.notifyStudentOnNewAssignment(solutionId, assessment)
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
  }
}
