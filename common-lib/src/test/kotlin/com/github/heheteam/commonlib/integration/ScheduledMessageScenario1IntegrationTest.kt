package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.testdouble.StudentBotTelegramControllerTestDouble
import com.github.heheteam.commonlib.util.TestDataBuilder
import com.github.heheteam.commonlib.util.buildData
import com.github.heheteam.commonlib.util.defaultInstant
import com.github.heheteam.commonlib.util.defaultTimezone
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import io.mockk.clearAllMocks
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private fun atLocal(duration: Duration): LocalDateTime {
  return defaultInstant.plus(duration).toLocalDateTime(defaultTimezone)
}

data class TestScenarioData(
  val admin: Admin,
  val student1: Student,
  val student2: Student,
  val course: Course,
  val messageContent: TelegramMessageContent,
)

/**
 * This integration test suite covers the full lifecycle of a scheduled message: scheduling by an
 * admin, initial 'not sent' state verification, triggering delivery, confirming 'sent' status, and
 * finally, verifying that all enrolled students receive the correct message content.
 */
class ScheduledMessageScenario1IntegrationTest : IntegrationTestEnvironment() {

  override val studentBotController = StudentBotTelegramControllerTestDouble()

  @BeforeEach
  fun setupMocks() {
    studentBotController.clearState()
    clearAllMocks()
  }

  /**
   * Sets up a common test scenario context with an admin, two students, a course with these
   * students, and a default Telegram message content. Note: `admin`, `student`, and `course`
   * functions are available within the `buildData` context.
   *
   * @return [TestScenarioData] containing the initialized entities.
   */
  suspend fun TestDataBuilder.setupCommonScheduledMessageTestContext(): TestScenarioData {
    val admin = admin("Admin1", "Admin1", 100L)
    val student1 = student("Student1", "Student1", 200L)
    val student2 = student("Student2", "Student2", 201L)
    val course =
      course("Course1") {
        withStudent(student1)
        withStudent(student2)
      }
    val messageContent = TelegramMessageContent.fromString("Hello students!")
    return TestScenarioData(admin, student1, student2, course, messageContent)
  }

  fun TestDataBuilder.scheduleMessage(
    context: TestScenarioData,
    scheduledTimestamp: LocalDateTime,
    shortName: String,
  ): ScheduledMessage {
    val scheduledMessageIdResult =
      sendScheduledMessage(
        adminId = context.admin.id,
        timestamp = scheduledTimestamp,
        content = context.messageContent,
        shortName = shortName,
        courseId = context.course.id,
      )
    return resolveScheduledMessage(scheduledMessageIdResult.value).value
  }

  suspend fun TestDataBuilder.triggerMessageDelivery(
    deliveryTimestamp: LocalDateTime
  ): Result<Unit, EduPlatformError> {
    return checkAndSentMessages(deliveryTimestamp)
  }

  fun getMessagesForStudent(studentId: RawChatId): Map<MessageId, TelegramMessageContent> {
    return studentBotController.getSentMessages(studentId).orEmpty()
  }

  @Test
  fun `scheduled message is initially not sent`() = runTest {
    buildData(createDefaultApis()) {
      val context = setupCommonScheduledMessageTestContext()
      val scheduledTimestamp = atLocal(1.minutes)

      val initialScheduledMessage = scheduleMessage(context, scheduledTimestamp, "Welcome Message")

      assertFalse(initialScheduledMessage.isSent, "Scheduled message should not be sent initially.")
    }
  }

  @Test
  fun `scheduled message is marked as sent after delivery`() = runTest {
    buildData(createDefaultApis()) {
      val context = setupCommonScheduledMessageTestContext()
      val scheduledTimestamp = atLocal(1.minutes)
      val initialScheduledMessage = scheduleMessage(context, scheduledTimestamp, "Welcome Message")
      val deliveryTimestamp = atLocal(2.minutes)

      val checkAndSendResult = triggerMessageDelivery(deliveryTimestamp)
      val updatedScheduledMessage = resolveScheduledMessage(initialScheduledMessage.id).value

      assertTrue(checkAndSendResult.isOk, "checkAndSentMessages should return OK.")
      assertTrue(
        updatedScheduledMessage.isSent,
        "Scheduled message should be marked as sent after delivery.",
      )
    }
  }

  @Test
  fun `students receive scheduled message after delivery`() = runTest {
    buildData(createDefaultApis()) {
      val context = setupCommonScheduledMessageTestContext()
      val scheduledTimestamp = atLocal(1.minutes)
      scheduleMessage(context, scheduledTimestamp, "Welcome Message")
      val deliveryTimestamp = atLocal(2.minutes)

      triggerMessageDelivery(deliveryTimestamp)
      val student1Messages = getMessagesForStudent(context.student1.tgId)
      val student2Messages = getMessagesForStudent(context.student2.tgId)

      assertEquals(1, student1Messages.size, "Student 1 should have received 1 message.")
      assertEquals(1, student2Messages.size, "Student 2 should have received 1 message.")

      assertEquals(
        context.messageContent.text,
        student1Messages.values.first().text,
        "Student 1 message content mismatch.",
      )
      assertEquals(
        context.messageContent.text,
        student2Messages.values.first().text,
        "Student 2 message content mismatch.",
      )
    }
  }
}
