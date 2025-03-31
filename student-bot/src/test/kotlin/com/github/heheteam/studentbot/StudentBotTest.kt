package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
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
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.ui.UiController
import com.github.heheteam.commonlib.notifications.BotEventBus
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import io.mockk.mockk
import java.time.LocalDateTime
import korlibs.time.fromMinutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach

class StudentBotTest {
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var solutionDistributor: SolutionDistributor
  private lateinit var studentApi: StudentApi
  private lateinit var courseIds: List<CourseId>
  private lateinit var gradeTable: GradeTable
  private lateinit var studentStorage: StudentStorage
  private lateinit var teacherStorage: TeacherStorage
  private lateinit var problemStorage: ProblemStorage
  private lateinit var assignmentStorage: AssignmentStorage
  private lateinit var academicWorkflowLogic: AcademicWorkflowLogic
  private lateinit var academicWorkflowService: AcademicWorkflowService

  private fun createAssignment(courseId: CourseId): List<Problem> {
    val assignment =
      assignmentStorage.createAssignment(
        courseId,
        "",
        listOf(
          ProblemDescription(1, "1", "", 1),
          ProblemDescription(2, "2", "", 1),
          ProblemDescription(3, "3", "", 1),
          ProblemDescription(4, "4", "", 1),
          ProblemDescription(5, "5", "", 1),
        ),
        problemStorage,
      )
    return problemStorage.getProblemsFromAssignment(assignment)
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
    reset(database)
    coursesDistributor = DatabaseCoursesDistributor(database)
    solutionDistributor = DatabaseSolutionDistributor(database)
    studentStorage = DatabaseStudentStorage(database)
    assignmentStorage = DatabaseAssignmentStorage(database)
    studentStorage = DatabaseStudentStorage(database)
    teacherStorage = DatabaseTeacherStorage(database)
    problemStorage = DatabaseProblemStorage(database)
    gradeTable = DatabaseGradeTable(database)
    academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, gradeTable)

    courseIds = (1..4).map { coursesDistributor.createCourse("course $it") }

    val mockBotEventBus = mockk<BotEventBus>(relaxed = true)
    val mockUiController = mockk<UiController>(relaxed = true)
    academicWorkflowService =
      AcademicWorkflowService(
        academicWorkflowLogic,
        RandomTeacherResolver(problemStorage, assignmentStorage, coursesDistributor),
        mockBotEventBus,
        mockUiController,
      )

    studentApi =
      StudentApi(coursesDistributor, problemStorage, assignmentStorage, academicWorkflowService)
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = studentStorage.createStudent()

    val studentCourses = studentApi.getStudentCourses(studentId)
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.id })
  }

  @Test
  fun `new student courses handling test`() {
    val studentId = studentStorage.createStudent()

    studentApi.applyForCourse(studentId, courseIds[0])
    studentApi.applyForCourse(studentId, courseIds[3])

    val studentCourses = studentApi.getStudentCourses(studentId)

    assertEquals(
      listOf(courseIds[0], courseIds[3]),
      studentCourses.map { it.id }.sortedBy { it.id },
    )
    assertEquals(listOf("course 1", "course 4"), studentCourses.map { it.name }.toList())
  }

  @Test
  fun `send solution test`() {
    val solutions = mutableListOf<SolutionId>()
    val chatId = RawChatId(0)

    val courseId = courseIds.first()
    val teacherId = teacherStorage.createTeacher()
    val userId = studentStorage.createStudent()
    coursesDistributor.addStudentToCourse(userId, courseId)
    coursesDistributor.addTeacherToCourse(teacherId, courseId)

    var time = kotlinx.datetime.LocalDateTime(2000, 12, 1, 12, 0, 0)
    createAssignment(courseId).forEach { problem ->
      studentApi.inputSolution(
        SolutionInputRequest(
          userId,
          problem.id,
          SolutionContent(text = "sample${problem.number}"),
          TelegramMessageInfo(chatId, MessageId(problem.id.id)),
          time,
        )
      )
      time =
        (time.toInstant(TimeZone.UTC) + Duration.fromMinutes(1.0)).toLocalDateTime(TimeZone.UTC)
    }

    repeat(5) {
      val solution = solutionDistributor.querySolution(teacherId).value
      println(solution)
      if (solution != null) {
        solutions.add(solution.id)
        gradeTable.recordSolutionAssessment(
          solution.id,
          teacherId,
          SolutionAssessment(5, "comment"),
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
      }
    }

    val firstSolutionResult = solutionDistributor.resolveSolution(solutions.first())
    assertTrue(firstSolutionResult.isOk)
    val firstSolution = firstSolutionResult.value
    assertEquals("sample1", firstSolution.content.text)

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
