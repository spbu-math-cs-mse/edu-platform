package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.ApiFabric
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.api.TeacherResolverKind
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.interfaces.toProblemId
import com.github.heheteam.commonlib.interfaces.toSolutionId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.heheteam.commonlib.util.MonotoneDummyClock
import com.github.michaelbull.result.Ok
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

  private val defaultMessageInfo = TelegramMessageInfo(RawChatId(0L), MessageId(0L))
  private val defaultMessageInfoNum = { num: Int ->
    TelegramMessageInfo(RawChatId(num.toLong()), MessageId(num.toLong()))
  }
  private val clock = MonotoneDummyClock()
  private val defaultTimestamp = clock.next()

  @Test
  fun `telegram notifications are sent on new solution`() {
    val apis = createDefaultApis()
    coEvery { teacherBotTelegramController.sendInitSolutionStatusMessageDM(any(), any()) } returns
      Ok(defaultMessageInfoNum(1))
    coEvery {
      teacherBotTelegramController.sendInitSolutionStatusMessageInCourseGroupChat(any(), any())
    } returns Ok(defaultMessageInfoNum(1))
    apis.studentApi.inputSolution(
      SolutionInputRequest(
        studentId = 1L.toStudentId(),
        problemId = 1L.toProblemId(),
        TextWithMediaAttachments(),
        defaultMessageInfo,
        defaultTimestamp,
      )
    )
    coVerify { teacherBotTelegramController.sendInitSolutionStatusMessageDM(any(), any()) }
  }

  private fun sendSolution(studentApi: StudentApi) {
    coEvery { teacherBotTelegramController.sendInitSolutionStatusMessageDM(any(), any()) } returns
      Ok(defaultMessageInfoNum(1))
    coEvery {
      teacherBotTelegramController.sendInitSolutionStatusMessageInCourseGroupChat(any(), any())
    } returns Ok(defaultMessageInfoNum(1))
    studentApi.inputSolution(
      SolutionInputRequest(
        studentId = 1L.toStudentId(),
        problemId = 1L.toProblemId(),
        TextWithMediaAttachments(),
        defaultMessageInfo,
        defaultTimestamp,
      )
    )
  }

  @Test
  fun `telegram notifications are sent on new assessment`() {
    val apis = createDefaultApis()
    sendSolution(apis.studentApi)
    sendSolution(apis.studentApi)
    coEvery {
      studentBotTelegramController.notifyStudentOnNewAssessment(any(), any(), any(), any(), any())
    } returns Unit
    apis.teacherApi.assessSolution(
      1L.toSolutionId(),
      1L.toTeacherId(),
      SolutionAssessment(1),
      defaultTimestamp,
    )
    coVerify {
      studentBotTelegramController.notifyStudentOnNewAssessment(any(), any(), any(), any(), any())
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
        initDatabase = true,
        useRedis = false,
        teacherResolverKind = TeacherResolverKind.FIRST,
      )
}
