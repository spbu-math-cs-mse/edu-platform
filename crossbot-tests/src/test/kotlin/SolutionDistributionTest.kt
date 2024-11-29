package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class SolutionDistributionTest {
  private lateinit var coursesDistributor: MockCoursesDistributor
  private lateinit var solutionDistributor: InMemorySolutionDistributor
  private lateinit var teacherStatistics: InMemoryTeacherStatistics
  private lateinit var gradeTable: InMemoryGradeTable
  private val problemStorage: ProblemStorage = InMemoryProblemStorage()
  private val assignmentStorage: AssignmentStorage = InMemoryAssignmentStorage()

  private lateinit var teacherCore: TeacherCore

  private lateinit var studentCore: StudentCore

  @BeforeEach
  fun setup() {
    coursesDistributor = MockCoursesDistributor()
    solutionDistributor = InMemorySolutionDistributor()
    teacherStatistics = InMemoryTeacherStatistics()
    gradeTable = InMemoryGradeTable()

    studentCore =
      StudentCore(
        solutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        InMemoryGradeTable(),
      )
  }

  @Test
  fun `solution distribution with existing student test`() {
    val teacherId = TeacherId(0L)

    teacherCore =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        InMemoryGradeTable(),
      )

    val userId = coursesDistributor.singleUserId
    val chatId = RawChatId(100)
    val messageId = MessageId(10)
    val text = "sample solution\nwith lines\n..."

    val problemId = ProblemId(239L)
    studentCore.inputSolution(
      StudentId(userId),
      chatId,
      messageId,
      SolutionContent(text = text),
      problemId,
    )

    val extractedSolution =
      solutionDistributor.resolveSolution(
        solutionDistributor.querySolution(teacherId)!!,
      )
    assertNotNull(extractedSolution)
    val expectedText =
      """sample solution
      |with lines
      |...
      """.trimMargin()
    assertEquals(expectedText, extractedSolution.content.text)
    assertEquals(problemId, extractedSolution.problemId)
    assertEquals(messageId, extractedSolution.messageId)

    val solution = teacherCore.querySolution(teacherId)
    assertNotNull(solution)

    assertEquals(messageId, teacherCore.querySolution(teacherId)?.messageId)

    assertEquals(extractedSolution.content.text, solution.content.text)
    assertEquals(extractedSolution.chatId, solution.chatId)

    teacherCore.assessSolution(
      solution,
      teacherId,
      SolutionAssessment(5, "way to go"),
    )

    val emptySolution = teacherCore.querySolution(teacherId)
    assertNull(emptySolution)
  }

  @Test
  fun `solution distribution with new student test`() {
    val teacherId = TeacherId(654L)

    teacherCore =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        InMemoryGradeTable(),
      )

    val userId = StudentId(255L)
    val chatId = RawChatId(200)
    val messageId = MessageId(15)
    val text = SolutionType.PHOTOS.toString()
    val fileIds = listOf("2", "3", "4")
    val problemId = ProblemId(239L)
    studentCore.inputSolution(
      userId,
      chatId,
      messageId,
      SolutionContent(fileIds = fileIds, text = text),
      problemId,
    )

    val extractedSolution =
      solutionDistributor.resolveSolution(
        solutionDistributor.querySolution(teacherId)!!,
      )
    assertNotNull(extractedSolution)
    assertEquals(text, "PHOTOS")
    assertEquals(problemId, extractedSolution.problemId)
    assertEquals(SolutionType.PHOTOS, extractedSolution.type)
    assertEquals(messageId, extractedSolution.messageId)

    val solution = teacherCore.querySolution(teacherId)
    assertNotNull(solution)

    assertEquals(fileIds, solution.content.fileIds)

    teacherCore.assessSolution(
      solution,
      teacherId,
      SolutionAssessment(3, "not too bad"),
    )

    val emptySolution = teacherCore.querySolution(teacherId)
    assertNull(emptySolution)
  }

  @Test
  fun `solution distribution with multiple test`() {
    val teacherId = TeacherId(1337L)
    val teacherId2 = TeacherId(1338L)

    teacherCore =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        InMemoryGradeTable(),
      )

    val teacherCore2 =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        InMemoryGradeTable(),
      )

    val userId1 = StudentId(coursesDistributor.singleUserId)
    var id = 10L
    var problemId = 0L
    val chatId1 = RawChatId(88)
    val fileIds1 = listOf("8")

    run {
      repeat(3) {
        val messageId = MessageId(id)
        val text = "solution ${id++}"
        studentCore.inputSolution(
          userId1,
          chatId1,
          messageId,
          SolutionContent(text = text),
          ProblemId(problemId++),
        )
      }
      studentCore.inputSolution(
        userId1,
        chatId1,
        MessageId(101),
        SolutionContent(
          fileIds = fileIds1,
          text = SolutionType.DOCUMENT.toString(),
        ),
        ProblemId(problemId++),
      )
    }

    val solution1 = teacherCore2.querySolution(teacherId2)
    assertNotNull(solution1)

    teacherCore.assessSolution(
      solution1,
      teacherId,
      SolutionAssessment(2, "bad"),
    )

    val solution2 = teacherCore2.querySolution(teacherId2)
    assertNotNull(solution2)

    assertEquals("solution 11", solution2.content.text)
    assertEquals(SolutionId(2L), solution2.id)

    teacherCore2.assessSolution(
      solution2,
      teacherId2,
      SolutionAssessment(3, "ok"),
    )

    val userId2 = StudentId(3L)
    val chatId2 = RawChatId(90)
    val messageId2 = MessageId(203)
    val text2 = "another solution"

    run {
      studentCore.inputSolution(
        userId2,
        chatId2,
        messageId2,
        SolutionContent(text = text2),
        ProblemId(problemId++),
      )
    }

    val solution3 = teacherCore.querySolution(teacherId)
    assertNotNull(solution3)

    assertNull(solution3.content.fileIds)
    assertEquals(chatId1, solution3.chatId)

    teacherCore.assessSolution(
      solution3,
      teacherId,
      SolutionAssessment(4, "good"),
    )

    val solution4 = teacherCore.querySolution(teacherId)
    assertNotNull(solution4)
    assertEquals("DOCUMENT", solution4.type.toString())

    assertEquals(fileIds1, solution4.content.fileIds)

    teacherCore.assessSolution(
      solution4,
      teacherId,
      SolutionAssessment(4, "good"),
    )

    val solution5 = teacherCore2.querySolution(teacherId2)
    assertNotNull(solution5)

    assertNotEquals(userId1, solution5.studentId)
    assertEquals(text2, solution5.content.text)
    assertEquals(messageId2, solution5.messageId)

    teacherCore2.assessSolution(
      solution5,
      teacherId2,
      SolutionAssessment(5, "great!"),
    )
  }
}
