package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.TelegramError
import com.github.heheteam.commonlib.logic.SubmissionSendingResult
import com.github.heheteam.commonlib.telegram.SubmissionStatusMessageInfo
import com.github.heheteam.commonlib.util.buildData
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import io.mockk.coEvery
import io.mockk.coVerify
import kotlin.test.Test
import kotlin.test.assertIs
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
    } returns Unit.ok()

  private fun mockSendMenuMessage(returnValue: Result<TelegramMessageInfo, EduPlatformError>) =
    coEvery { teacherBotController.sendMenuMessageInPersonalChat(any(), any()) } returns returnValue

  @Test
  fun `telegram notifications are sent on new submission`() = runTest {
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
  fun `student api inputSubmission propagates telegram errors`() = runTest {
    coEvery { teacherBotController.sendSubmission(any(), any()) } returns
      Err(TelegramError(RuntimeException("Simulated Telegram error during sendSubmission")))
    mockSendInitSubmissionStatusMessageDM(Ok(messageInfoNum(1)))
    mockSendInitSubmissionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
    mockSendMenuMessage(Ok(messageInfoNum(1)))

    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val teacher = teacher("Teacher1", "Teacher1")

      course("Course1") {
        withStudent(student)
        withTeacher(teacher)
        val (_, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val submissionInputRequest =
          submissionInputRequest(student, problems.first(), "Submission1")
        val result = apis.studentApi.inputSubmission(submissionInputRequest)

        assertIs<SubmissionSendingResult.Success>(result)
        assertIs<EduPlatformError>(
          result.newSubmissionNotificationStatus.teacherNewSubmissionNotificationStatus
            .teacherDirectMessagingSendError
        )
      }
    }
  }

  @Test
  fun `teacher api updateTeacherMenuMessage propagates telegram errors`() = runTest {
    mockSendInitSubmissionStatusMessageDM(Ok(messageInfoNum(1)))
    mockSendInitSubmissionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
    coEvery { teacherBotController.sendMenuMessageInPersonalChat(any(), any()) } returns
      Err(TelegramError(RuntimeException("Simulated Telegram error during sendMenuMessage")))

    buildData(createDefaultApis()) {
      val teacher = teacher("Teacher1", "Teacher1")
      val result = apis.teacherApi.updateTeacherMenuMessage(teacher.id)
      assertIs<EduPlatformError>(result.getError())
    }
  }

  @Test
  fun `telegram notifications are sent on new assessment`() = runTest {
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
        val (assignment, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val submission = submission(student, problems.first(), "Submission1")
        val assessment = assessment(teacher, submission, 1)

        coVerify(exactly = 1) {
          studentBotController.notifyStudentOnNewAssessment(
            chatId = student.tgId,
            messageToReplyTo = submission.messageId,
            problem = problems.first(),
            assignment = assignment,
            assessment = assessment,
          )
        }
      }
    }
  }
}
