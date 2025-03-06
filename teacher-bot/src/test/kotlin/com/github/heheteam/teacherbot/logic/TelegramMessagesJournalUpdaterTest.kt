package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.toTeacherId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import kotlin.test.Test
import kotlinx.datetime.toKotlinLocalDateTime

class TelegramMessagesJournalUpdaterTest {
  val solutionId = SolutionId(1L)
  val defaultTimestamp = LocalDateTime.of(2000, 1, 1, 12, 0)
  val good = SolutionAssessment(1)
  val bad = SolutionAssessment(0)

  val gradings =
    listOf(
      GradingEntry(0L.toTeacherId(), good, defaultTimestamp.toKotlinLocalDateTime()),
      GradingEntry(1L.toTeacherId(), bad, defaultTimestamp.plusMinutes(1L).toKotlinLocalDateTime()),
    )

  @Test
  fun `Journal updater informs group bot`() {
    val gradeTable = mockk<GradeTable>(relaxed = true)
    val technicalMessageService = mockk<TechnicalMessageUpdater>(relaxed = true)
    every { gradeTable.getGradingsForSolution(solutionId) } returns gradings
    val journalUpdater = TelegramMessagesJournalUpdater(gradeTable, technicalMessageService)
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
    verify { technicalMessageService.updateTechnicalMessageInGroup(solutionId, gradings) }
  }

  @Test
  fun `Journal updater informs personal bot`() {
    val gradeTable = mockk<GradeTable>(relaxed = true)
    val technicalMessageService = mockk<TechnicalMessageUpdater>(relaxed = true)
    every { gradeTable.getGradingsForSolution(solutionId) } returns gradings
    val journalUpdater = TelegramMessagesJournalUpdater(gradeTable, technicalMessageService)
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
    verify { technicalMessageService.updateTechnnicalMessageInPersonalChat(solutionId, gradings) }
  }
}
