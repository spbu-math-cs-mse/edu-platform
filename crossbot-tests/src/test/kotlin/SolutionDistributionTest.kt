package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.mock.MockBotEventBus
import com.github.heheteam.commonlib.mock.MockNotificationService
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class SolutionDistributionTest {
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var solutionDistributor: SolutionDistributor
  private lateinit var teacherStatistics: TeacherStatistics
  private lateinit var gradeTable: GradeTable
  private lateinit var problemStorage: ProblemStorage
  private lateinit var assignmentStorage: AssignmentStorage
  private lateinit var studentStorage: StudentStorage
  private lateinit var teacherStorage: TeacherStorage
  private lateinit var teacherCore: TeacherCore
  private lateinit var studentCore: StudentCore

  private var messageId = 1L

  fun newMessageId(): MessageId = MessageId(messageId++)

  @BeforeEach
  fun setup() {
    val config = loadConfig()

    val database = Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )
    coursesDistributor = DatabaseCoursesDistributor(database)
    solutionDistributor = DatabaseSolutionDistributor(database)
    teacherStatistics = InMemoryTeacherStatistics()
    gradeTable = DatabaseGradeTable(database)
    assignmentStorage = DatabaseAssignmentStorage(database)
    studentStorage = DatabaseStudentStorage(database)
    teacherStorage = DatabaseTeacherStorage(database)
    problemStorage = DatabaseProblemStorage(database)
    studentCore =
      StudentCore(
        solutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        gradeTable,
        MockNotificationService(),
        MockBotEventBus(),
      )
  }

  @Ignore
  @Test
  fun `solution distribution with existing student and teacher test`() {
    val teacherId = teacherStorage.createTeacher()
    teacherCore =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        gradeTable,
        problemStorage,
        MockBotEventBus(),
        assignmentStorage,
        studentStorage,
      )

    val studentId = studentStorage.createStudent()
    val chatId = RawChatId(studentId.id)
    val messageId = newMessageId()
    val text = "sample solution\nwith lines\n..."

    val courseId = coursesDistributor.createCourse("")
    coursesDistributor.addStudentToCourse(studentId, courseId)

    val problemId = createProblem(courseId)
    studentCore.inputSolution(
      studentId,
      chatId,
      messageId,
      SolutionContent(text = text),
      problemId,
    )

    assertNull(solutionDistributor.querySolution(teacherId, gradeTable).value)

    coursesDistributor.addTeacherToCourse(teacherId, courseId)

    val extractedSolutionResult =
      solutionDistributor.resolveSolution(
        solutionDistributor.querySolution(teacherId, gradeTable).value!!.id,
      )
    assertTrue(extractedSolutionResult.isOk)
    val extractedSolution = extractedSolutionResult.value
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

  private fun createProblem(courseId: CourseId = coursesDistributor.createCourse("")): ProblemId {
    val assignment =
      assignmentStorage.createAssignment(
        courseId,
        "",
        listOf(ProblemDescription("1", "", 1)),
        problemStorage,
      )
    val problemId = problemStorage.getProblemsFromAssignment(assignment).first().id
    return problemId
  }

  @Ignore // because documents are not yet supported in database
  @Test
  fun `solution distribution with new student test`() {
    val teacherId = TeacherId(654L)
    teacherCore =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        gradeTable,
        problemStorage,
        MockBotEventBus(),
        assignmentStorage,
        studentStorage,
      )

    val userId = studentStorage.createStudent()
    val chatId = RawChatId(userId.id)
    val messageId = newMessageId()
    val text = SolutionType.GROUP.toString()
    val fileIds = listOf("2", "3", "4")
    val problemId = createProblem()
    studentCore.inputSolution(
      userId,
      chatId,
      messageId,
      SolutionContent(filesURL = fileIds, text = text),
      problemId,
    )

    val extractedSolutionResult =
      solutionDistributor.resolveSolution(
        solutionDistributor.querySolution(teacherId, gradeTable).value!!.id,
      )
    assertTrue(extractedSolutionResult.isOk)
    val extractedSolution = extractedSolutionResult.value
    assertEquals(text, "PHOTOS")
    assertEquals(problemId, extractedSolution.problemId)
//    assertEquals(SolutionType.PHOTOS, extractedSolution.type)
    assertEquals(messageId, extractedSolution.messageId)

    val solution = teacherCore.querySolution(teacherId)
    assertNotNull(solution)

    assertEquals(fileIds, solution.content.filesURL)

    teacherCore.assessSolution(
      solution,
      teacherId,
      SolutionAssessment(3, "not too bad"),
    )

    val emptySolution = teacherCore.querySolution(teacherId)
    assertNull(emptySolution)
  }

  @Ignore // because documents are not yet supported
  @Test
  fun `solution distribution with multiple test`() {
    val teacherId = TeacherId(1337L)
    val teacherId2 = TeacherId(1338L)

    teacherCore =
      TeacherCore(
        teacherStatistics,
        coursesDistributor,
        solutionDistributor,
        gradeTable,
        problemStorage,
        MockBotEventBus(),
        assignmentStorage,
        studentStorage,
      )

    val userId1 = studentStorage.createStudent()
    var id = 10L
    val chatId1 = RawChatId(88)
    val fileIds1 = listOf("8")
    // send (text="solution{id++}) x3, then document
    run {
      repeat(3) {
        val messageId = MessageId(id)
        val text = "solution ${id++}"
        studentCore.inputSolution(
          userId1,
          chatId1,
          messageId,
          SolutionContent(text = text),
          createProblem(),
        )
      }
      studentCore.inputSolution(
        userId1,
        chatId1,
        MessageId(101),
        SolutionContent(
          filesURL = fileIds1,
          text = SolutionType.DOCUMENT.toString(),
        ),
        createProblem(),
      )
    }

    val solution1 = teacherCore.querySolution(teacherId2)
    println(solution1?.content)
    assertNotNull(solution1)
    teacherCore.assessSolution(
      solution1,
      teacherId,
      SolutionAssessment(2, "bad"),
    )

    val solution2 = teacherCore.querySolution(teacherId2)
    assertNotNull(solution2)

    assertEquals("solution 11", solution2.content.text)
    assertEquals(SolutionId(2L), solution2.id)

    teacherCore.assessSolution(
      solution2,
      teacherId2,
      SolutionAssessment(3, "ok"),
    )

    val userId2 = studentStorage.createStudent()
    val chatId2 = RawChatId(90)
    val messageId2 = MessageId(203)
    val text2 = "another solution"

    run {
      studentCore.inputSolution(
        userId2,
        chatId2,
        messageId2,
        SolutionContent(text = text2),
        createProblem(),
      )
    }

    val solution3 = teacherCore.querySolution(teacherId)
    assertNotNull(solution3)

//    assertNull(solution3.content.fileIds)
    assertEquals(chatId1, solution3.chatId)

    teacherCore.assessSolution(
      solution3,
      teacherId,
      SolutionAssessment(4, "good"),
    )

    val solution4 = teacherCore.querySolution(teacherId)
    assertNotNull(solution4)
    assertEquals("DOCUMENT", solution4.type.toString())

    assertEquals(fileIds1, solution4.content.filesURL)

    teacherCore.assessSolution(
      solution4,
      teacherId,
      SolutionAssessment(4, "good"),
    )

    val solution5 = teacherCore.querySolution(teacherId2)
    assertNotNull(solution5)

    assertNotEquals(userId1, solution5.studentId)
    assertEquals(text2, solution5.content.text)
    assertEquals(messageId2, solution5.messageId)

    teacherCore.assessSolution(
      solution5,
      teacherId2,
      SolutionAssessment(5, "great!"),
    )
  }
}
