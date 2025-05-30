package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.googlesheets.RawCourseSheetData
import com.github.heheteam.commonlib.telegram.SubmissionStatusMessageInfo
import com.github.heheteam.commonlib.util.buildData
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class AcademicWorkflowTest : IntegrationTestEnvironment() {
  private fun mockSendInitSubmissionStatusMessageDM(
    returnValue: Result<TelegramMessageInfo, EduPlatformError>
  ) =
    coEvery { teacherBotController.sendInitSubmissionStatusMessageDM(any(), any()) } returns
      returnValue

  private fun mockSendInitSubmissionStatusMessageInCourseGroupChat(
    returnValue: Result<TelegramMessageInfo, EduPlatformError>
  ) =
    coEvery {
      teacherBotController.sendInitSubmissionStatusMessageInCourseGroupChat(any(), any())
    } returns returnValue

  private fun mockNotifyStudentOnNewAssessment() =
    coEvery {
      studentBotController.notifyStudentOnNewAssessment(any(), any(), any(), any(), any())
    } returns Unit

  private fun mockSendMenuMessage(returnValue: Result<TelegramMessageInfo, EduPlatformError>) =
    coEvery { teacherBotController.sendMenuMessage(any(), any()) } returns returnValue

  private fun mockUpdateRating() = justRun { googleSheetsService.updateRating(any(), any(), any()) }

  @Test
  fun `telegram notifications are sent on new submission`() {
    mockSendInitSubmissionStatusMessageDM(Ok(messageInfoNum(1)))
    mockSendInitSubmissionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
    mockSendMenuMessage(Ok(messageInfoNum(1)))

    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val teacher = teacher("Teacher1", "Teacher1")

      course("Course1") {
        withStudent(student)
        withTeacher(teacher)
        val (assignment, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val submission = submission(student, problems.first(), "Submission1")

        coVerify {
          teacherBotController.sendInitSubmissionStatusMessageDM(
            chatId = teacher.tgId,
            submissionStatusMessageInfo =
              SubmissionStatusMessageInfo(
                submissionId = submission.id,
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
    mockSendInitSubmissionStatusMessageDM(Ok(messageInfoNum(1)))
    mockSendInitSubmissionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
    mockSendMenuMessage(Ok(messageInfoNum(1)))
    mockNotifyStudentOnNewAssessment()

    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val teacher = teacher("Teacher1", "Teacher1")

      course("Course1") {
        withStudent(student)
        withTeacher(teacher)
        val (_, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val submission = submission(student, problems.first(), "Submission1")
        val assessment = assessment(teacher, submission, 1)

        coVerify(exactly = 1) {
          studentBotController.notifyStudentOnNewAssessment(
            chatId = student.tgId,
            messageToReplyTo = submission.messageId,
            studentId = student.id,
            problem = problems.first(),
            assessment = assessment,
          )
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
  @Test
  fun `google table is updated on new assessment with correct performance`() =
    runTest(testDispatcher) {
      mockSendInitSubmissionStatusMessageDM(Ok(messageInfoNum(1)))
      mockSendInitSubmissionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
      mockSendMenuMessage(Ok(messageInfoNum(1)))
      mockNotifyStudentOnNewAssessment()
      mockUpdateRating()
      buildData(createDefaultApis()) {
        val student = student("Student1", "Student1")
        val teacher = teacher("Teacher1", "Teacher1")
        course("Course1") {
          setChat(13L)
          withStudent(student)
          withTeacher(teacher)
          val problem = assignment("Assignment1") { problem("Problem1", 10) }.second.first()
          val submission = submission(student, problem, "Submission1")
          awaitCoroutineScheduler()
          clearMocks(googleSheetsService)
          val assessment = assessment(teacher, submission, 1)
          awaitCoroutineScheduler()
          val performanceSlot = slot<RawCourseSheetData>()
          verify(exactly = 1) {
            googleSheetsService.updateRating(any(), any(), capture(performanceSlot))
          }
          val expectedPerformance = mapOf(student.id to mapOf(problem.id to assessment.grade))
          val actualPerformance = performanceSlot.captured.performance
          assertEquals(expectedPerformance, actualPerformance)
        }
      }
    }

  private fun TestScope.awaitCoroutineScheduler() {
    testScheduler.runCurrent()
    testScheduler.advanceUntilIdle()
  }
}
