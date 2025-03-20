package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.CoreServicesInitializer
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
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
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.util.SampleGenerator
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin

class StudentBotTest : KoinComponent {
  companion object {
    @JvmStatic
    @BeforeAll
    fun initKoin() {
      startKoin { modules(CoreServicesInitializer().inject(useRedis = false)) }
    }

    @JvmStatic
    @AfterAll
    fun stopKoinAfterAll() {
      stopKoin()
    }
  }

  private val coursesDistributor: CoursesDistributor by inject()
  private val solutionDistributor: SolutionDistributor by inject()
  private val gradeTable: GradeTable by inject()
  private val studentStorage: StudentStorage by inject()
  private val teacherStorage: TeacherStorage by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()

  private lateinit var courseIds: List<CourseId>
  private lateinit var studentCore: StudentCore

  private fun createProblem(courseId: CourseId = coursesDistributor.createCourse("")): ProblemId {
    val assignment =
      assignmentStorage.createAssignment(
        courseId,
        "",
        listOf(ProblemDescription(1, "1", "", 1)),
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
    courseIds = SampleGenerator().fillWithSamples().courses
    studentCore = StudentCore()
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
    assertEquals(listOf("Начала мат. анализа", "ТФКП"), studentCourses.map { it.name }.toList())
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
          SolutionContent(text = "sample$it"),
          createProblem(courseId),
        )
      }

      repeat(5) {
        val solution = solutionDistributor.querySolution(teacherId).value
        if (solution != null) {
          solutions.add(solution.id)
          gradeTable.recordSolutionAssessment(
            solution.id,
            teacherId,
            SolutionAssessment(5, "comment"),
            LocalDateTime.now(),
          )
        }
      }
    }

    val firstSolutionResult = solutionDistributor.resolveSolution(solutions.first())
    assertTrue(firstSolutionResult.isOk)
    val firstSolution = firstSolutionResult.value
    assertEquals("sample0", firstSolution.content.text)

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
