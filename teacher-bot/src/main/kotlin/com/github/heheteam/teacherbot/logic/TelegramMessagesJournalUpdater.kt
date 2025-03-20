package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TelegramMessagesJournalUpdater : JournalUpdater, KoinComponent {
  private val gradeTable: GradeTable by inject()
  private val technicalMessageService: TechnicalMessageUpdater by inject()

  override fun updateJournalDisplaysForSolution(solutionId: SolutionId) {
    val gradings = gradeTable.getGradingsForSolution(solutionId)
    technicalMessageService.updateTechnicalMessageInGroup(solutionId, gradings)
    technicalMessageService.updateTechnnicalMessageInPersonalChat(solutionId, gradings)
  }
}
