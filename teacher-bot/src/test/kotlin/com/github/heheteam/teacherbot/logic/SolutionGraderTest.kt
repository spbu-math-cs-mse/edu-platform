package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.toSolutionId
import com.github.heheteam.commonlib.api.toTeacherId
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class SolutionGraderTest {
  val defaultTimestamp: LocalDateTime = LocalDateTime.of(2000, 1, 1, 12, 0)
  val good = SolutionAssessment(1)

  val solutionId = 0L.toSolutionId()
  val teacherId = 0L.toTeacherId()

  @Test
  fun `ideal solution test`() {
    val gradeTable = mockk<GradeTable>(relaxed = true)
    val uiController = mockk<UiController>(relaxed = true)
    val solutionGrader = SolutionGrader(gradeTable, uiController)
    solutionGrader.assessSolution(solutionId, teacherId, good, defaultTimestamp)
    verify { uiController.updateUiOnSolutionAssessment(solutionId, good) }
  }

  @Test
  fun `telegram solution properly notifies the student`() {
    val studentNotifier = mockk<StudentNewGradeNotifier>(relaxed = true)
    val journalUpdater = mockk<JournalUpdater>(relaxed = true)
    val menuMessageUpdater = mockk<MenuMessageUpdater>(relaxed = true)
    val uiControllerTelegramSender =
      UiControllerTelegramSender(studentNotifier, journalUpdater, menuMessageUpdater)
    uiControllerTelegramSender.updateUiOnSolutionAssessment(solutionId, good)
    verify { studentNotifier.notifyStudentOnNewAssessment(solutionId, good) }
  }

  @Test
  fun `telegram solution properly notifies the teachers`() {
    val studentNotifier = mockk<StudentNewGradeNotifier>(relaxed = true)
    val journalUpdater = mockk<JournalUpdater>(relaxed = true)
    val menuMessageUpdater = mockk<MenuMessageUpdater>(relaxed = true)
    val uiControllerTelegramSender =
      UiControllerTelegramSender(studentNotifier, journalUpdater, menuMessageUpdater)
    uiControllerTelegramSender.updateUiOnSolutionAssessment(solutionId, good)
    verify { journalUpdater.updateJournalDisplaysForSolution(solutionId) }
  }
}
