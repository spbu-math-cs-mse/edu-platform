package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.SolutionId

class TelegramMessagesJournalUpdater(
  private val gradeTable: GradeTable,
  private val technicalMessageService: SolutionMessageUpdater,
) : JournalUpdater {
  override fun updateJournalDisplaysForSolution(solutionId: SolutionId) {
    val gradings = gradeTable.getGradingsForSolution(solutionId)
    technicalMessageService.updateSolutionMessageInGroup(solutionId, gradings)
    technicalMessageService.updateSolutionMessageInPersonalChat(solutionId, gradings)
  }
}
