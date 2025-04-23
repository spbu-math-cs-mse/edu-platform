package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.ApiFabric
import com.github.heheteam.commonlib.api.TeacherResolverKind
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.telegram.SolutionStatusMessageInfo
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.heheteam.commonlib.util.buildData
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import org.jetbrains.exposed.sql.Database

class ApiTests {
  private val config = loadConfig()
  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  private val googleSheetsService = mockk<GoogleSheetsService>(relaxed = true)
  private val studentBotTelegramController = mockk<StudentBotTelegramController>(relaxed = true)
  private val teacherBotTelegramController = mockk<TeacherBotTelegramController>(relaxed = true)

  private val defaultMessageInfoNum = { num: Int ->
    TelegramMessageInfo(RawChatId(num.toLong()), MessageId(num.toLong()))
  }

  private fun mockSendInitSolutionStatusMessageDM(returnValue: Result<TelegramMessageInfo, Any>) =
    coEvery { teacherBotTelegramController.sendInitSolutionStatusMessageDM(any(), any()) } returns
      returnValue

  private fun mockSendInitSolutionStatusMessageInCourseGroupChat(
    returnValue: Result<TelegramMessageInfo, Any>
  ) =
    coEvery {
      teacherBotTelegramController.sendInitSolutionStatusMessageInCourseGroupChat(any(), any())
    } returns returnValue

  private fun mockNotifyStudentOnNewAssessment() =
    coEvery {
      studentBotTelegramController.notifyStudentOnNewAssessment(any(), any(), any(), any(), any())
    } returns Unit

  private fun mockSendMenuMessage(returnValue: Result<TelegramMessageInfo, Any>) =
    coEvery { teacherBotTelegramController.sendMenuMessage(any(), any()) } returns returnValue

  @Test
  fun `telegram notifications are sent on new solution`() {
    mockSendInitSolutionStatusMessageDM(Ok(defaultMessageInfoNum(1)))
    mockSendInitSolutionStatusMessageInCourseGroupChat(Ok(defaultMessageInfoNum(1)))
    mockSendMenuMessage(Ok(defaultMessageInfoNum(1)))

    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val teacher = teacher("Teacher1", "Teacher1")

      course("Course1") {
        withStudent(student)
        withTeacher(teacher)
        val (assignment, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val solution = solution(student, problems.first(), "Solution1")

        coVerify {
          teacherBotTelegramController.sendInitSolutionStatusMessageDM(
            chatId = teacher.tgId,
            solutionStatusMessageInfo =
              SolutionStatusMessageInfo(
                solutionId = solution.id,
                assignmentDisplayName = assignment.description,
                problemDisplayName = problems.first().number,
                student = student,
                responsibleTeacher = teacher,
                gradingEntries = listOf(),
              ),
          )
        }
      }
    }
  }

  @Test
  fun `telegram notifications are sent on new assessment`() {
    mockSendInitSolutionStatusMessageDM(Ok(defaultMessageInfoNum(1)))
    mockSendInitSolutionStatusMessageInCourseGroupChat(Ok(defaultMessageInfoNum(1)))
    mockSendMenuMessage(Ok(defaultMessageInfoNum(1)))
    mockNotifyStudentOnNewAssessment()

    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val teacher = teacher("Teacher1", "Teacher1")

      course("Course1") {
        withStudent(student)
        withTeacher(teacher)
        val (_, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val solution = solution(student, problems.first(), "Solution1")
        val assessment = assessment(teacher, solution, 1)

        coVerify(exactly = 1) {
          studentBotTelegramController.notifyStudentOnNewAssessment(
            chatId = student.tgId,
            messageToReplyTo = solution.messageId,
            studentId = student.id,
            problem = problems.first(),
            assessment = assessment,
          )
        }
      }
    }
  }

  private fun createDefaultApis() =
    ApiFabric(
        database,
        config,
        googleSheetsService,
        studentBotTelegramController,
        teacherBotTelegramController,
      )
      .createApis(
        initDatabase = false,
        useRedis = false,
        teacherResolverKind = TeacherResolverKind.FIRST,
      )
}
