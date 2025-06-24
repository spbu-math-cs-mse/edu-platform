package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.testdouble.StudentBotTelegramControllerTestDouble
import com.github.heheteam.commonlib.util.buildData
import com.github.heheteam.commonlib.util.defaultInstant
import com.github.heheteam.commonlib.util.defaultTimezone
import io.mockk.clearAllMocks
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
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
  fun `scenario 2 - message visibility via viewSentMessages`() = runTest {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course1 = course("Course1") { withStudent(student("Student1", "Student1", 200L)) }
      val course2 = course("Course2") { withStudent(student("Student2", "Student2", 201L)) }

      val messageContent1 = tgMsg("Message 1")
      val messageContent2 = tgMsg("Message 2")
      val messageContent3 = tgMsg("Message 3")

      val msg1Id =
        sendScheduledMessage(admin.id, at((-2).hours), messageContent1, "Msg1", course1.id).value
      val msg2Id =
        sendScheduledMessage(admin.id, at((-1).hours), messageContent2, "Msg2", course2.id).value
      val msg3Id =
        sendScheduledMessage(admin.id, at((-30).minutes), messageContent3, "Msg3", course1.id).value

      checkAndSentMessages(at(0.seconds)).value

      val sentMessages = viewRecordedMessages(adminId = admin.id, limit = 3).value
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
  fun `scenario 3 - message resolution before delivery`() = runTest {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course = course("Course1") { withStudent(student("Student1", "Student1", 200L)) }
      val content = tgMsg("Test Resolution")
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
  fun `scenario 3 - message resolution after delivery`() = runTest {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course = course("Course1") { withStudent(student("Student1", "Student1", 200L)) }
      val content = tgMsg("Test Resolution")
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
  fun `scenario 4 - message deletion and student message cleanup`() = runTest {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val student1 = student("Student1", "Student1", 200L)
      val student2 = student("Student2", "Student2", 201L)
      val course =
        course("Course1") {
          withStudent(student1)
          withStudent(student2)
        }
      val messageContent = tgMsg("Message to delete")
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

  @Test
  fun `scenario 5 - viewScheduledMessages filters by adminId`() = runTest {
    buildData(createDefaultApis()) {
      val admin1 = admin("Admin1", "Admin1", 100L)
      val admin2 = admin("Admin2", "Admin2", 101L)
      val course1 = course("Course1")
      val course2 = course("Course2")

      sendScheduledMessage(admin1.id, at(1.hours), tgMsg("Msg A"), "MsgA", course1.id)
      sendScheduledMessage(admin2.id, at(2.hours), tgMsg("Msg B"), "MsgB", course2.id)
      sendScheduledMessage(admin1.id, at(3.hours), tgMsg("Msg C"), "MsgC", course1.id)

      val messagesForAdmin1 = viewRecordedMessages(adminId = admin1.id, limit = 3).value
      assertEquals(2, messagesForAdmin1.size)
      assertTrue(messagesForAdmin1.all { it.adminId == admin1.id })
      assertEquals("MsgC", messagesForAdmin1[0].shortName)
      assertEquals("MsgA", messagesForAdmin1[1].shortName)
    }
  }

  @Test
  fun `scenario 6 - viewScheduledMessages filters by courseId`() = runTest {
    buildData(createDefaultApis()) {
      val admin = admin("Admin1", "Admin1", 100L)
      val course1 = course("Course1")
      val course2 = course("Course2")

      sendScheduledMessage(admin.id, at(1.hours), tgMsg("Msg A"), "MsgA", course1.id)
      sendScheduledMessage(admin.id, at(2.hours), tgMsg("Msg B"), "MsgB", course2.id)
      sendScheduledMessage(admin.id, at(3.hours), tgMsg("Msg C"), "MsgC", course1.id)

      val messagesForCourse1 = viewRecordedMessages(courseId = course1.id, limit = 3).value
      assertEquals(2, messagesForCourse1.size)
      assertTrue(messagesForCourse1.all { it.courseId == course1.id })
      assertEquals("MsgC", messagesForCourse1[0].shortName)
      assertEquals("MsgA", messagesForCourse1[1].shortName)
    }
  }

  fun tgMsg(s: String): TextWithMediaAttachments = TextWithMediaAttachments.fromString(s)

  @Test
  fun `scenario 7 - viewScheduledMessages filters by adminId and courseId`() = runTest {
    buildData(createDefaultApis()) {
      val admin1 = admin("Admin1", "Admin1", 100L)
      val admin2 = admin("Admin2", "Admin2", 101L)
      val course1 = course("Course1")
      val course2 = course("Course2")
      sendScheduledMessage(admin1.id, at(1.hours), tgMsg("Msg A"), "MsgA", course1.id)
      sendScheduledMessage(admin2.id, at(2.hours), tgMsg("Msg B"), "MsgB", course2.id)
      sendScheduledMessage(admin1.id, at(3.hours), tgMsg("Msg C"), "MsgC", course1.id)
      sendScheduledMessage(admin1.id, at(4.hours), tgMsg("Msg D"), "MsgD", course2.id)

      val messagesFiltered =
        viewRecordedMessages(adminId = admin1.id, courseId = course1.id, limit = 3).value
      assertEquals(2, messagesFiltered.size)
      assertTrue(messagesFiltered.all { it.adminId == admin1.id && it.courseId == course1.id })
      assertEquals("MsgC", messagesFiltered[0].shortName)
      assertEquals("MsgA", messagesFiltered[1].shortName)
    }
  }

  @Test
  fun `scenario 8 - viewScheduledMessages with no filters returns all messages`() = runTest {
    buildData(createDefaultApis()) {
      val admin1 = admin("Admin1", "Admin1", 100L)
      val admin2 = admin("Admin2", "Admin2", 101L)
      val course1 = course("Course1")
      val course2 = course("Course2")

      sendScheduledMessage(admin1.id, at(1.hours), tgMsg("Msg A"), "MsgA", course1.id)
      sendScheduledMessage(admin2.id, at(2.hours), tgMsg("Msg B"), "MsgB", course2.id)
      sendScheduledMessage(admin1.id, at(3.hours), tgMsg("Msg C"), "MsgC", course1.id)

      val allMessages = viewRecordedMessages(limit = 3).value
      assertEquals(3, allMessages.size)
      assertEquals("MsgC", allMessages[0].shortName)
      assertEquals("MsgB", allMessages[1].shortName)
      assertEquals("MsgA", allMessages[2].shortName)
    }
  }

  @Test
  fun `deleted message is seen in recent scheduled messages`() = runTest {
    buildData(createDefaultApis()) {
      val admin = admin("Admin2", "Admin2", 101L)
      val course = course("Course2")
      val msg = sendScheduledMessage(admin.id, at(2.hours), tgMsg("Msg B"), "MsgB", course.id).value
      deleteScheduledMessage(msg)
      val allMessages = viewRecordedMessages(limit = 3).value
      val single = allMessages.single()
      assertEquals(msg, single.id)
      assertEquals(true, single.isDeleted)
    }
  }
}
