package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.util.buildData
import io.mockk.coVerify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class DeadlinesManagementTest : IntegrationTestEnvironment() {
  @Test
  fun `telegram notifications are sent on new moving deadlines request`() = runTest {
    val apis = createDefaultApis()
    buildData(apis) {
      val student = student("Student1", "Student1")
      val admin = admin("Student1", "Student1")

      val newDeadline =
        Clock.System.now()
          .plus(2, DateTimeUnit.MINUTE)
          .toLocalDateTime(TimeZone.currentSystemDefault())
      movingDeadlinesRequest(student, newDeadline)

      coVerify {
        adminBotController.notifyAdminOnNewMovingDeadlinesRequest(
          admin.tgId,
          student.id,
          newDeadline,
        )
      }
    }
  }

  @Test
  fun `telegram notifications are sent on moving deadlines`() = runTest {
    val apis = createDefaultApis()
    buildData(apis) {
      val student = student("Student1", "Student1")

      course("Course1") {
        withStudent(student)

        val newDeadline =
          Clock.System.now()
            .plus(2, DateTimeUnit.MINUTE)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        movingDeadlinesRequest(student, newDeadline)
        moveDeadlines(student, newDeadline)

        coVerify {
          studentBotController.notifyStudentOnDeadlineRescheduling(student.tgId, newDeadline)
        }
      }
    }
  }

  @Test
  fun `deadlines are moved correctly`() = runTest {
    val apis = createDefaultApis()
    buildData(apis) {
      val student = student("Student1", "Student1")

      course("Course1") {
        withStudent(student)

        val (_, problems) =
          assignment("Assignment1") {
            problem("Problem1", 10, nowPlusNMinutes(1))
            problem("Problem2", 10, nowPlusNMinutes(2))
            problem("Problem3", 10, nowPlusNMinutes(3))
            problem("Problem4", 10)
          }

        val newDeadline = nowPlusNMinutes(2)
        movingDeadlinesRequest(student, newDeadline)
        moveDeadlines(student, newDeadline)

        val rescheduledProblems =
          apis.studentApi.calculateRescheduledDeadlines(student.id, problems)
        problems.zip(rescheduledProblems).forEach { (original, rescheduled) ->
          assertEquals(original.id, rescheduled.id)
          assertEquals(original.serialNumber, rescheduled.serialNumber)
          assertEquals(original.number, rescheduled.number)
          assertEquals(original.description, rescheduled.description)
          assertEquals(original.maxScore, rescheduled.maxScore)
          assertEquals(original.assignmentId, rescheduled.assignmentId)
          val originalDeadline = original.deadline
          val rescheduledDeadline = rescheduled.deadline
          if (originalDeadline == null || rescheduledDeadline == null) {
            assertEquals(originalDeadline, rescheduledDeadline)
          } else if (originalDeadline > newDeadline) {
            assertTimeApproximatelyEquals(originalDeadline, rescheduledDeadline)
          } else {
            assertTimeApproximatelyEquals(newDeadline, rescheduledDeadline)
          }
        }
      }
    }
  }

  private fun nowPlusNMinutes(n: Int) =
    Clock.System.now().plus(n, DateTimeUnit.MINUTE).toLocalDateTime(TimeZone.currentSystemDefault())

  private fun assertTimeApproximatelyEquals(expected: LocalDateTime, actual: LocalDateTime) {
    val expectedInstant = expected.toInstant(TimeZone.currentSystemDefault())
    val actualInstant = actual.toInstant(TimeZone.currentSystemDefault())
    val diff = (expectedInstant - actualInstant).absoluteValue
    assert(diff <= 1.seconds) {
      "Expected time approximately $expected but was $actual (difference: $diff)"
    }
  }
}
