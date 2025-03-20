package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.CoreServicesInitializer
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.teacherbot.run.TeacherBotServicesInitializer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import kotlin.test.Test
import kotlinx.datetime.toKotlinLocalDateTime
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class TelegramMessagesJournalUpdaterTest : KoinTest {
  private val gradeTable: GradeTable by inject()
  private val technicalMessageService: TechnicalMessageUpdater by inject()
  private val journalUpdater: TelegramMessagesJournalUpdater by inject()

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
      single { mockk<TechnicalMessageUpdater>(relaxed = true) }
      single { TelegramMessagesJournalUpdater() }
    }
  }

  val solutionId = SolutionId(1L)
  val defaultTimestamp: LocalDateTime = LocalDateTime.of(2000, 1, 1, 12, 0)
  val good = SolutionAssessment(1)
  val bad = SolutionAssessment(0)

  val gradings =
    listOf(
      GradingEntry(0L.toTeacherId(), good, defaultTimestamp.toKotlinLocalDateTime()),
      GradingEntry(1L.toTeacherId(), bad, defaultTimestamp.plusMinutes(1L).toKotlinLocalDateTime()),
    )

  @Test
  fun `Journal updater informs group bot`() {
    every { gradeTable.getGradingsForSolution(solutionId) } returns gradings
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
    verify { technicalMessageService.updateTechnicalMessageInGroup(solutionId, gradings) }
  }

  @Test
  fun `Journal updater informs personal bot`() {
    every { gradeTable.getGradingsForSolution(solutionId) } returns gradings
    journalUpdater.updateJournalDisplaysForSolution(solutionId)
    verify { technicalMessageService.updateTechnnicalMessageInPersonalChat(solutionId, gradings) }
  }
}
