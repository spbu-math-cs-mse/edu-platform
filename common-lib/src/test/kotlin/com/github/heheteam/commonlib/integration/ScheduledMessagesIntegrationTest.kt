package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.testdouble.StudentBotTelegramControllerTestDouble
import com.github.heheteam.commonlib.util.buildData
import com.github.heheteam.commonlib.util.defaultInstant
import com.github.heheteam.commonlib.util.defaultTimezone
import dev.inmo.tgbotapi.types.UserId
import io.mockk.clearAllMocks
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private fun at(duration: Duration): LocalDateTime {
  return defaultInstant.plus(duration).toLocalDateTime(defaultTimezone)
}

class ScheduledMessagesIntegrationTest : IntegrationTestEnvironment() {

  override val studentBotController = StudentBotTelegramControllerTestDouble()

  @BeforeEach
  fun setupMocks() {
    studentBotController.clearState()
    clearAllMocks()
  }

  @Test
  fun `scenario 2 - message visibility via viewSentMessages`() {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course1 = course("Course1") { withStudent(student("Student1", "Student1", 200L)) }
      val course2 = course("Course2") { withStudent(student("Student2", "Student2", 201L)) }

      val messageContent1 = TelegramMessageContent(text = "Message 1")
      val messageContent2 = TelegramMessageContent(text = "Message 2")
      val messageContent3 = TelegramMessageContent(text = "Message 3")

      val msg1Id =
        sendScheduledMessage(admin.id, at((-2).hours), messageContent1, "Msg1", course1.id).value
      val msg2Id =
        sendScheduledMessage(admin.id, at((-1).hours), messageContent2, "Msg2", course2.id).value
      val msg3Id =
        sendScheduledMessage(admin.id, at((-30).minutes), messageContent3, "Msg3", course1.id).value

      checkAndSentMessages(at(0.seconds)).value

      val sentMessages = viewRecordedMessages(UserId(admin.tgId), 3)
      assertEquals(3, sentMessages.size)
      assertEquals(msg3Id, sentMessages[0].id)
      assertEquals(msg2Id, sentMessages[1].id)
      assertEquals(msg1Id, sentMessages[2].id)

      assertEquals("Msg3", sentMessages[0].shortName)
      assertEquals(messageContent3.text, sentMessages[0].content.text)
      assertTrue(sentMessages[0].isSent)
    }
  }

  @Test
  fun `scenario 3 - message resolution before delivery`() {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course = course("Course1") { withStudent(student("Student1", "Student1", 200L)) }
      val content = TelegramMessageContent(text = "Test Resolution")
      val scheduledTimestamp = at(1.minutes)

      val scheduledMessageId =
        sendScheduledMessage(admin.id, scheduledTimestamp, content, "Resolution Test", course.id)
          .value

      val baseExpectedMessage =
        ScheduledMessage(
          id = scheduledMessageId,
          timestamp = scheduledTimestamp,
          content = content,
          shortName = "Resolution Test",
          courseId = course.id,
          isSent = false,
          isDeleted = false,
          adminId = admin.id,
        )

      val messageBeforeDelivery = resolveScheduledMessage(scheduledMessageId).value
      assertEquals(baseExpectedMessage, messageBeforeDelivery)
    }
  }

  @Test
  fun `scenario 3 - message resolution after delivery`() {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course = course("Course1") { withStudent(student("Student1", "Student1", 200L)) }
      val content = TelegramMessageContent(text = "Test Resolution")
      val scheduledTimestamp = at(1.minutes)

      val scheduledMessageId =
        sendScheduledMessage(admin.id, scheduledTimestamp, content, "Resolution Test", course.id)
          .value

      val baseExpectedMessage =
        ScheduledMessage(
          id = scheduledMessageId,
          timestamp = scheduledTimestamp,
          content = content,
          shortName = "Resolution Test",
          courseId = course.id,
          isSent = false,
          isDeleted = false,
          adminId = admin.id,
        )

      checkAndSentMessages(at(2.minutes)).value

      val messageAfterDelivery = resolveScheduledMessage(scheduledMessageId).value
      val expectedMessageAfterDelivery = baseExpectedMessage.copy(isSent = true)
      assertEquals(expectedMessageAfterDelivery, messageAfterDelivery)
    }
  }

  @Test
  fun `scenario 4 - message deletion and student message cleanup`() {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val student1 = student("Student1", "Student1", 200L)
      val student2 = student("Student2", "Student2", 201L)
      val course =
        course("Course1") {
          withStudent(student1)
          withStudent(student2)
        }
      val messageContent = TelegramMessageContent(text = "Message to delete")
      val scheduledTimestamp = at((-1).hours)

      val scheduledMessageId =
        sendScheduledMessage(
            admin.id,
            scheduledTimestamp,
            messageContent,
            "Deletion Test",
            course.id,
          )
          .value

      checkAndSentMessages(at(1.seconds)).value

      val deleteResult = deleteScheduledMessage(scheduledMessageId)
      assertTrue(deleteResult.isOk)

      val deletedMessage = resolveScheduledMessage(scheduledMessageId).value
      assertTrue(deletedMessage.isSent)
    }
  }
}
