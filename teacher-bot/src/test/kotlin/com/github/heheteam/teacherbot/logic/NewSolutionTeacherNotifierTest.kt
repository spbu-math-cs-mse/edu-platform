package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.Teacher
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
import org.junit.jupiter.api.BeforeEach

class NewSolutionTeacherNotifierTest {
  private val teacherId = TeacherId(0)
  val teacherTgId = RawChatId(5)
  val teacher = Teacher(teacherId, "", "", tgId = teacherTgId)
  val sampleMessageInfo = TelegramMessageInfo(RawChatId(1L), MessageId(2L))
  val solutionId = SolutionId(6)
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

  @Test
  fun `teacher notifier sends solution to responsible teacher`() {
    val telegramSolutionSender = mockk<TelegramSolutionSender>(relaxed = true)
    val technicalMessageStorage = mockk<TelegramTechnicalMessagesStorage>(relaxed = true)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(teacherStorage, telegramSolutionSender, technicalMessageStorage)
    newSolutionTeacherNotifier.notifyNewSolution(solution)
    verify { telegramSolutionSender.sendPersonalSolutionNotification(teacherTgId, solution) }
  }

  @Test
  fun `personal messages sent to telegram gets recorded`() {
    val telegramSolutionSender = mockk<TelegramSolutionSender>(relaxed = true)
    every { telegramSolutionSender.sendPersonalSolutionNotification(teacherTgId, solution) } returns
      Ok(sampleMessageInfo)

    val technicalMessageStorage = mockk<TelegramTechnicalMessagesStorage>(relaxed = true)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(teacherStorage, telegramSolutionSender, technicalMessageStorage)
    newSolutionTeacherNotifier.notifyNewSolution(solution)

    verify {
      technicalMessageStorage.registerPersonalSolutionPublication(solutionId, sampleMessageInfo)
    }
  }
}
