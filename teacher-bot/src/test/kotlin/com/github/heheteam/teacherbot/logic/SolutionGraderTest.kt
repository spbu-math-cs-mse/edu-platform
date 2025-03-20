package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.CoreServicesInitializer
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.toSolutionId
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.teacherbot.run.TeacherBotServicesInitializer
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class SolutionGraderTest : KoinTest {
  private val uiController: UiController by inject()
  private val studentNotifier: StudentNewGradeNotifier by inject()
  private val journalUpdater: JournalUpdater by inject()
  val uiControllerTelegramSender: UiControllerTelegramSender by inject()

  companion object {
    @JvmStatic
    @BeforeAll
    fun startKoinBeforeAll() {
      startKoin {
        modules(CoreServicesInitializer().inject(useRedis = false))
        modules(TeacherBotServicesInitializer().inject())
        modules(testModule)
      }
    }

    @JvmStatic
    @AfterAll
    fun stopKoinAfterAll() {
      stopKoin()
    }

    private val testModule = module {
      single { mockk<GradeTable>(relaxed = true) }
      single { mockk<UiController>(relaxed = true) }
      single { mockk<StudentNewGradeNotifier>(relaxed = true) }
      single { mockk<JournalUpdater>(relaxed = true) }
      single { UiControllerTelegramSender() }
    }
  }

  val defaultTimestamp: LocalDateTime = LocalDateTime.of(2000, 1, 1, 12, 0)
  val good = SolutionAssessment(1)

  val solutionId = 0L.toSolutionId()
  val teacherId = 0L.toTeacherId()

  @Test
  fun `ideal solution test`() {
    val solutionGrader = SolutionGrader()
    solutionGrader.assessSolution(solutionId, teacherId, good, defaultTimestamp)
    verify { uiController.updateUiOnSolutionAssessment(solutionId, good) }
  }

  @Test
  fun `telegram solution properly notifies the student`() {
    uiControllerTelegramSender.updateUiOnSolutionAssessment(solutionId, good)
    verify { studentNotifier.notifyStudentOnNewAssignment(solutionId, good) }
  }

  @Test
  fun `telegram solution properly notifies the teachers`() {
    uiControllerTelegramSender.updateUiOnSolutionAssessment(solutionId, good)
    verify { journalUpdater.updateJournalDisplaysForSolution(solutionId) }
  }
}
