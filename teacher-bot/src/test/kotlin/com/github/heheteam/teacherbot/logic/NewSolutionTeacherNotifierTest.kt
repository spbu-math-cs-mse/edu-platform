package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.table.TelegramMessageInfo
import com.github.heheteam.commonlib.database.table.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Ok
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class NewSolutionTeacherNotifierTest : KoinTest {
  private val telegramSolutionSender: TelegramSolutionSender by inject()
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage by inject()

  companion object {
    private val teacherId = TeacherId(0)
    private val teacherTgId = RawChatId(5)
    private val teacher = Teacher(teacherId, "", "", tgId = teacherTgId)
    private val samplePersonalMessageInfo = TelegramMessageInfo(RawChatId(1L), MessageId(2L))
    private val sampleGroupMessageInfo = TelegramMessageInfo(RawChatId(8L), MessageId(13L))
    private val solutionId = SolutionId(6)
    private val courseIdOfSolution = CourseId(7)

    private val testModule = module {
      single {
        mockk<Solution>(relaxed = true).apply {
          every { responsibleTeacherId } returns teacherId
          every { id } returns solutionId
        }
      }
      single {
        mockk<TeacherStorage>(relaxed = true).apply {
          every { resolveTeacher(teacherId) } returns Ok(teacher)
        }
      }
      single {
        mockk<SolutionCourseResolver>().apply {
          every { resolveCourse(solutionId) } returns Ok(courseIdOfSolution)
        }
      }
      single {
        mockk<TelegramSolutionSender>(relaxed = true).apply {
          every { sendPersonalSolutionNotification(teacherId, get<Solution>()) } returns
            Ok(samplePersonalMessageInfo)
          every { sendGroupSolutionNotification(courseIdOfSolution, get<Solution>()) } returns
            Ok(sampleGroupMessageInfo)
        }
      }
      single { mockk<TelegramTechnicalMessagesStorage>(relaxed = true) }
    }

    @JvmStatic
    @BeforeAll
    fun startKoinBeforeAll() {
      startKoin { modules(testModule) }
    }

    @JvmStatic
    @AfterAll
    fun stopKoinAfterAll() {
      stopKoin()
    }
  }

  private val solution: Solution by inject()

  @Test
  fun `teacher notifier sends solution to responsible teacher`() {
    val newSolutionTeacherNotifier = NewSolutionTeacherNotifier()

    newSolutionTeacherNotifier.notifyNewSolution(solution)

    verify { telegramSolutionSender.sendPersonalSolutionNotification(teacherId, solution) }
  }

  @Test
  fun `personal messages sent to teacher dm gets recorded`() {
    val newSolutionTeacherNotifier = NewSolutionTeacherNotifier()

    newSolutionTeacherNotifier.notifyNewSolution(solution)

    verify {
      technicalMessageStorage.registerPersonalSolutionPublication(
        solutionId,
        samplePersonalMessageInfo,
      )
    }
  }

  @Test
  fun `group messages are getting sent`() {
    val newSolutionTeacherNotifier = NewSolutionTeacherNotifier()

    newSolutionTeacherNotifier.notifyNewSolution(solution)

    verify { telegramSolutionSender.sendGroupSolutionNotification(courseIdOfSolution, solution) }
  }
}
