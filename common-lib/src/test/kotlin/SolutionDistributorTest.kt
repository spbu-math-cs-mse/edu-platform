package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlin.test.Test
import kotlin.test.assertEquals

class SolutionDistributorTest {
  @Test
  fun `teacher gets user solution TEXT`() {
    val studentId = "student"
    val teacherId = "teacher"
    val mockSolutionDistributor = MockSolutionDistributor()
    mockSolutionDistributor.inputSolution(studentId, RawChatId(0), MessageId(0), SolutionContent(text = "test"))
    val solution = mockSolutionDistributor.querySolution(teacherId)!!
    assertEquals(studentId, solution.studentId)
    assertEquals(SolutionContent(text = "test"), solution.content)
    assertEquals(SolutionType.TEXT, solution.type)
    assertEquals(MessageId(0), solution.messageId)
    assertEquals(RawChatId(0), solution.chatId)
  }
}
