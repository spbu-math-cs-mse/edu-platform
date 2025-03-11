package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionId

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
