package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.TelegramAttachment
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentStorage
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
import com.github.heheteam.commonlib.util.fillWithSamples
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach

class StudentBotTest {
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var solutionDistributor: SolutionDistributor
  private lateinit var studentCore: StudentCore
  private lateinit var courseIds: List<CourseId>
  private lateinit var gradeTable: GradeTable
  private lateinit var studentStorage: StudentStorage
  private lateinit var teacherStorage: TeacherStorage
  private lateinit var problemStorage: ProblemStorage
  private lateinit var assignmentStorage: AssignmentStorage

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

  private val config = loadConfig()

  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  @BeforeEach
  fun setup() {
    coursesDistributor = DatabaseCoursesDistributor(database)
    solutionDistributor = DatabaseSolutionDistributor(database)
    studentStorage = DatabaseStudentStorage(database)
    assignmentStorage = DatabaseAssignmentStorage(database)
    studentStorage = DatabaseStudentStorage(database)
    teacherStorage = DatabaseTeacherStorage(database)
    problemStorage = DatabaseProblemStorage(database)
    courseIds =
      fillWithSamples(
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    gradeTable = DatabaseGradeTable(database)

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

  @AfterTest
  fun reset() {
    reset(database)
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = studentStorage.createStudent()

    val studentCourses = studentCore.getStudentCourses(studentId)
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.id })

    assertEquals("Вы не записаны ни на один курс!", studentCore.getCoursesBulletList(studentId))
  }

  @Test
  fun `new student courses handling test`() {
    val studentId = studentStorage.createStudent()

    studentCore.addRecord(studentId, courseIds[0])
    studentCore.addRecord(studentId, courseIds[3])

    val studentCourses = studentCore.getStudentCourses(studentId)

    assertEquals(
      listOf(courseIds[0], courseIds[3]),
      studentCourses.map { it.id }.sortedBy { it.id },
    )
    assertEquals("Начала мат. анализа", studentCourses.first().name)

    assertEquals("- Начала мат. анализа\n- ТФКП", studentCore.getCoursesBulletList(studentId))
  }

  @Test
  fun `send solution test`() {
    val solutions = mutableListOf<SolutionId>()
    val chatId = RawChatId(0)

    run {
      val courseId = courseIds.first()
      val teacherId = teacherStorage.createTeacher()
      val userId = studentStorage.createStudent()
      coursesDistributor.addStudentToCourse(userId, courseId)
      coursesDistributor.addTeacherToCourse(teacherId, courseId)

      (0..4).forEach {
        studentCore.inputSolution(
          userId,
          chatId,
          MessageId(it.toLong()),
          TelegramAttachment(text = "sample$it"),
          createProblem(courseId),
        )
      }

      repeat(5) {
        val solution = solutionDistributor.querySolution(teacherId).value
        if (solution != null) {
          solutions.add(solution.id)
          gradeTable.assessSolution(
            solution.id,
            teacherId,
            SolutionAssessment(5, "comment"),
            InMemoryTeacherStatistics(),
            LocalDateTime.now(),
          )
        }
      }
    }

    val firstSolutionResult = solutionDistributor.resolveSolution(solutions.first())
    assertTrue(firstSolutionResult.isOk)
    val firstSolution = firstSolutionResult.value
    assertEquals("sample0", firstSolution.attachments.text)

    val lastSolutionResult = solutionDistributor.resolveSolution(solutions.last())
    assertTrue(lastSolutionResult.isOk)
    val lastSolution = lastSolutionResult.value
    assertEquals(SolutionId(5L), lastSolution.id)
    assertEquals(
      solutions.map { solutionDistributor.resolveSolution(it).value.chatId }.toSet(),
      setOf(chatId),
    )
  }
}
