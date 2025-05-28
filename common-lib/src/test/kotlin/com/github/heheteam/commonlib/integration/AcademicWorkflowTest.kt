package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.telegram.SolutionStatusMessageInfo
import com.github.heheteam.commonlib.util.buildData
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.mockk.coEvery
import io.mockk.coVerify
import kotlin.test.Test

class AcademicWorkflowTest : IntegrationTestEnvironment() {
  private fun mockSendInitSolutionStatusMessageDM(returnValue: Result<TelegramMessageInfo, Any>) =
    coEvery { teacherBotController.sendInitSolutionStatusMessageDM(any(), any()) } returns
      returnValue

  private fun mockSendInitSolutionStatusMessageInCourseGroupChat(
    returnValue: Result<TelegramMessageInfo, Any>
  ) =
    coEvery {
      teacherBotController.sendInitSolutionStatusMessageInCourseGroupChat(any(), any())
    } returns returnValue

  private fun mockNotifyStudentOnNewAssessment() =
    coEvery {
      studentBotController.notifyStudentOnNewAssessment(any(), any(), any(), any(), any())
    } returns Unit

  private fun mockSendMenuMessage(returnValue: Result<TelegramMessageInfo, Any>) =
    coEvery { teacherBotController.sendMenuMessage(any(), any()) } returns returnValue

  @Test
  fun `telegram notifications are sent on new solution`() {
    mockSendInitSolutionStatusMessageDM(Ok(messageInfoNum(1)))
    mockSendInitSolutionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
    mockSendMenuMessage(Ok(messageInfoNum(1)))

    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val teacher = teacher("Teacher1", "Teacher1")

      course("Course1") {
        withStudent(student)
        withTeacher(teacher)
        val (assignment, problems) = assignment("Assignment1") { problem("Problem1", 10) }

        val solution = solution(student, problems.first(), "Solution1")

        coVerify {
          teacherBotController.sendInitSolutionStatusMessageDM(
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
    mockSendInitSolutionStatusMessageDM(Ok(messageInfoNum(1)))
    mockSendInitSolutionStatusMessageInCourseGroupChat(Ok(messageInfoNum(1)))
    mockSendMenuMessage(Ok(messageInfoNum(1)))
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
          studentBotController.notifyStudentOnNewAssessment(
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
}
