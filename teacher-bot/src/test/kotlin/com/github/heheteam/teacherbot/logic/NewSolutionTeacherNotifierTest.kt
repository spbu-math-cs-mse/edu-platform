package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Ok
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import org.junit.jupiter.api.BeforeEach

class NewSolutionTeacherNotifierTest {
  private val teacherId = TeacherId(0)
  val teacherTgId = RawChatId(5)
  val teacher = Teacher(teacherId, "", "", tgId = teacherTgId)
  val samplePersonalMessageInfo = TelegramMessageInfo(RawChatId(1L), MessageId(2L))
  val sampleGroupMessageInfo = TelegramMessageInfo(RawChatId(8L), MessageId(13L))
  val solutionId = SolutionId(6)
  val courseIdOfSolution = CourseId(7)

  lateinit var teacherStorage: TeacherStorage
  lateinit var solution: Solution

  @BeforeEach
  fun init() {
    solution = mockk<Solution>(relaxed = true)
    teacherStorage = mockk<TeacherStorage>(relaxed = true)
    every { solution.responsibleTeacherId } returns teacherId
    every { solution.id } returns solutionId
    every { teacherStorage.resolveTeacher(teacherId) } returns Ok(teacher)
  }

  private fun createSolutionResolver(): SolutionCourseResolver {
    val solutionCourseResolver = mockk<SolutionCourseResolver>()
    every { solutionCourseResolver.resolveCourse(solutionId) } returns Ok(courseIdOfSolution)
    return solutionCourseResolver
  }

  private fun createTgSolutionSender(): TelegramSolutionSender {
    val telegramSolutionSender = mockk<TelegramSolutionSender>(relaxed = true)
    every { telegramSolutionSender.sendPersonalSolutionNotification(teacherId, solution) } returns
      Ok(samplePersonalMessageInfo)
    every {
      telegramSolutionSender.sendGroupSolutionNotification(courseIdOfSolution, solution)
    } returns Ok(sampleGroupMessageInfo)
    return telegramSolutionSender
  }

  @Test
  fun `teacher notifier sends solution to responsible teacher`() {
    val telegramSolutionSender = createTgSolutionSender()
    val technicalMessageStorage = mockk<TelegramTechnicalMessagesStorage>(relaxed = true)
    val solutionCourseResolver = createSolutionResolver()
    val menuMessageUpdater = mockk<MenuMessageUpdater>(relaxed = true)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(
        telegramSolutionSender,
        technicalMessageStorage,
        solutionCourseResolver,
        menuMessageUpdater,
      )
    newSolutionTeacherNotifier.notifyNewSolution(solution)
    verify { telegramSolutionSender.sendPersonalSolutionNotification(teacherId, solution) }
  }

  @Test
  fun `personal messages sent to teacher dm gets recorded`() {
    val telegramSolutionSender = createTgSolutionSender()
    val technicalMessageStorage = mockk<TelegramTechnicalMessagesStorage>(relaxed = true)
    val solutionCourseResolver = createSolutionResolver()
    val menuMessageUpdater = mockk<MenuMessageUpdater>(relaxed = true)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(
        telegramSolutionSender,
        technicalMessageStorage,
        solutionCourseResolver,
        menuMessageUpdater,
      )
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
    val telegramSolutionSender = createTgSolutionSender()
    val technicalMessageStorage = mockk<TelegramTechnicalMessagesStorage>(relaxed = true)
    val menuMessageUpdater = mockk<MenuMessageUpdater>(relaxed = true)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(
        telegramSolutionSender,
        technicalMessageStorage,
        createSolutionResolver(),
        menuMessageUpdater,
      )
    newSolutionTeacherNotifier.notifyNewSolution(solution)
    verify { telegramSolutionSender.sendGroupSolutionNotification(courseIdOfSolution, solution) }
  }

  @Test
  fun `menu message gets updated`() {
    val telegramSolutionSender = createTgSolutionSender()
    val technicalMessageStorage = mockk<TelegramTechnicalMessagesStorage>(relaxed = true)
    val solutionCourseResolver = createSolutionResolver()
    val menuMessageUpdater = mockk<MenuMessageUpdater>(relaxed = true)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(
        telegramSolutionSender,
        technicalMessageStorage,
        solutionCourseResolver,
        menuMessageUpdater,
      )
    newSolutionTeacherNotifier.notifyNewSolution(solution)

    verify { menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId) }
  }
}
