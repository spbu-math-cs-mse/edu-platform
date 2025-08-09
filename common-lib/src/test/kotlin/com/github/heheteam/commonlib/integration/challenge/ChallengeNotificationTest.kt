package com.github.heheteam.commonlib.integration.challenge

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.buildData
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChallengeNotificationTest : IntegrationTestEnvironment() {
  @Test
  fun `all admins are notified on challenge access request`() = runTest {
    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val admin1 = admin("Admin1", "Admin1", 100L)
      val admin2 = admin("Admin2", "Admin2", 200L)
      val course =
        course("Course1") {
          withStudent(student)
          val (assignment1, _) = assignment("Assignment1") { problem("Problem1", 10) }

          challenge(assignment1, "Challenge1") { problem("HardProblem1", 20) }
        }

      apis.studentApi.requestChallengeAccess(student.id, course.id)

      coVerify(exactly = 1) {
        adminBotController.notifyAdminOnNewChallengeAccessRequest(
          chatId = admin1.tgId,
          studentId = student.id,
          courseId = course.id,
        )
      }

      coVerify(exactly = 1) {
        adminBotController.notifyAdminOnNewChallengeAccessRequest(
          chatId = admin2.tgId,
          studentId = student.id,
          courseId = course.id,
        )
      }
    }
  }

  @Test
  fun `student is notified on granted access to challenge`() = runTest {
    buildData(createDefaultApis()) {
      val student = student("Student", "Student", 100L)
      admin("Admin", "Admin")
      val course =
        course("Course1") {
          withStudent(student)
          val (assignment1, _) = assignment("Assignment1") { problem("Problem1", 10) }

          challenge(assignment1, "Challenge1") { problem("HardProblem1", 20) }
        }

      apis.adminApi.grantAccessToChallengeForStudent(student.id, course.id)

      coVerify {
        studentBotController.notifyStudentOnGrantedAccessToChallenge(
          chatId = student.tgId,
          course = course,
        )
      }
    }
  }
}
