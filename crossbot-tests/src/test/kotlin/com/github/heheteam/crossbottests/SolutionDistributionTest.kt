package com.github.heheteam.crossbottests

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.mock.MockBotEventBus
import com.github.heheteam.commonlib.mock.MockNotificationService
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach

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
    val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )
    reset(database)

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
  }

  @Test
  fun `solution distribution with simple text`() {
    val teacherId = teacherStorage.createTeacher()

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
      SolutionContent(text = text, type = SolutionType.TEXT),
      problemId,
    )

    assertNull(solutionDistributor.querySolution(teacherId).value)

    coursesDistributor.addTeacherToCourse(teacherId, courseId)

    val extractedSolutionResult =
      solutionDistributor.resolveSolution(solutionDistributor.querySolution(teacherId).value!!.id)
    assertTrue(extractedSolutionResult.isOk)
    val extractedSolution = extractedSolutionResult.value
    val expectedText =
      """sample solution
      |with lines
      |...
      """
        .trimMargin()
    assertEquals(expectedText, extractedSolution.content.text)
    assertEquals(problemId, extractedSolution.problemId)
    assertEquals(messageId, extractedSolution.messageId)

    val solution = teacherCore.querySolution(teacherId)
    assertNotNull(solution)

    assertEquals(messageId, teacherCore.querySolution(teacherId)?.messageId)

    assertEquals(extractedSolution.content.text, solution.content.text)
    assertEquals(extractedSolution.chatId, solution.chatId)

    teacherCore.assessSolution(solution, teacherId, SolutionAssessment(5, "way to go"))

    val emptySolution = teacherCore.querySolution(teacherId)
    assertNull(emptySolution)
  }

  @Test
  fun `solution distribution with files`() {
    val teacherId = teacherStorage.createTeacher()

    val studentId = studentStorage.createStudent()

    val courseId = coursesDistributor.createCourse("")

    coursesDistributor.addStudentToCourse(studentId, courseId)
    coursesDistributor.addTeacherToCourse(teacherId, courseId)

    val chatId = RawChatId(studentId.id)
    val messageId = newMessageId()
    val type = SolutionType.GROUP
    val fileURL = listOf("url1", "url2", "url3")
    val problemId = createProblem(courseId)

    studentCore.inputSolution(
      studentId,
      chatId,
      messageId,
      SolutionContent(filesURL = fileURL, type = type),
      problemId,
    )

    val extractedSolutionResult =
      solutionDistributor.resolveSolution(solutionDistributor.querySolution(teacherId).value!!.id)
    assertTrue(extractedSolutionResult.isOk)

    val extractedSolution = extractedSolutionResult.value

    assertEquals(problemId, extractedSolution.problemId)
    assertEquals(SolutionType.GROUP, extractedSolution.content.type)
    assertEquals(messageId, extractedSolution.messageId)

    val solution = teacherCore.querySolution(teacherId)
    assertNotNull(solution)

    assertEquals(fileURL, solution.content.filesURL)

    teacherCore.assessSolution(solution, teacherId, SolutionAssessment(3, "not too bad"))

    val emptySolution = teacherCore.querySolution(teacherId)
    assertNull(emptySolution)
  }

  @Test
  fun `complex solution distribution`() {
    val teacherId1 = teacherStorage.createTeacher()
    val teacherId2 = teacherStorage.createTeacher()

    val studentId1 = studentStorage.createStudent()
    val studentId2 = studentStorage.createStudent()

    val courseId1 = coursesDistributor.createCourse("course 1")
    val courseId2 = coursesDistributor.createCourse("course 2")

    coursesDistributor.addTeacherToCourse(teacherId1, courseId1)
    coursesDistributor.addTeacherToCourse(teacherId2, courseId2)
    coursesDistributor.addStudentToCourse(studentId1, courseId1)
    coursesDistributor.addStudentToCourse(studentId2, courseId2)

    var id = 10L
    val chatId1 = RawChatId(88)
    val chatId2 = RawChatId(96)

    // send (text="solution{id++}) x3, then document
    run {
      repeat(3) {
        val messageId = MessageId(id)
        val text = "solution ${id++}"
        studentCore.inputSolution(
          studentId1,
          chatId1,
          messageId,
          SolutionContent(text = text, type = SolutionType.TEXT),
          createProblem(courseId1),
        )
      }

      val fileURL2 = listOf("url1")
      studentCore.inputSolution(
        studentId2,
        chatId2,
        MessageId(101),
        SolutionContent(
          filesURL = fileURL2,
          text = "this is document",
          type = SolutionType.DOCUMENT,
        ),
        createProblem(courseId2),
      )
    }

    val solution1from1 = teacherCore.querySolution(teacherId1)
    assertNotNull(solution1from1)
    teacherCore.assessSolution(solution1from1, teacherId1, SolutionAssessment(2, "bad"))

    val solution1from2 = teacherCore.querySolution(teacherId2)
    assertNotNull(solution1from2)

    assertEquals(listOf("url1"), solution1from2.content.filesURL)
    assertEquals(SolutionId(4L), solution1from2.id)

    teacherCore.assessSolution(solution1from2, teacherId2, SolutionAssessment(3, "ok"))

    run {
      studentCore.inputSolution(
        studentId2,
        chatId2,
        MessageId(107),
        SolutionContent(text = "this is solution from student 2!", type = SolutionType.TEXT),
        createProblem(courseId2),
      )
    }

    val solution2from1 = teacherCore.querySolution(teacherId1)
    assertNotNull(solution2from1)
    assertEquals("solution 11", solution2from1.content.text)

    val solution2from2 = teacherCore.querySolution(teacherId2)
    assertNotNull(solution2from2)
    assertNotNull(solution2from2.content.text)
    assertTrue(!solution2from2.content.text!!.startsWith("solution"))

    teacherCore.assessSolution(solution2from1, teacherId1, SolutionAssessment(4, "good"))

    teacherCore.assessSolution(solution2from2, teacherId2, SolutionAssessment(4, "good"))

    val empty = teacherCore.querySolution(teacherId2)
    assertNull(empty)

    val solution3from1 = teacherCore.querySolution(teacherId1)
    assertNotNull(solution3from1)
    assertEquals("solution 12", solution3from1.content.text)
    assertEquals(SolutionType.TEXT, solution3from1.content.type)
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
}
