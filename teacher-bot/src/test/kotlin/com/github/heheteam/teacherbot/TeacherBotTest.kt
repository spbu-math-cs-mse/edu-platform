package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue

class TeacherBotTest {
  private val now = LocalDateTime.now()
  private lateinit var teacherId: TeacherId
  private lateinit var studentId: StudentId
  private lateinit var problemId: ProblemId
  private val config = loadConfig()

  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )
  private val solutionDistributor = DatabaseSolutionDistributor(database)
  private val coursesDistributor = DatabaseCoursesDistributor(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val statistics = InMemoryTeacherStatistics()

  private fun makeSolution(timestamp: LocalDateTime) =
    solutionDistributor.inputSolution(
      studentId,
      RawChatId(0L),
      MessageId(0L),
      SolutionContent(),
      problemId,
      timestamp,
    )

  @BeforeTest
  fun setUp() {
    reset(database)
    teacherId = teacherStorage.createTeacher()
    studentId = studentStorage.createStudent()
    val courseId = coursesDistributor.createCourse("test course")
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
    coursesDistributor.addStudentToCourse(studentId, courseId)
    val assignmentId =
      assignmentStorage.createAssignment(
        courseId,
        "test assignment",
        listOf(ProblemDescription(1, "p1", "", 1), ProblemDescription(2, "p2", "", 1)),
        DatabaseProblemStorage(database),
      )
    problemId = problemStorage.createProblem(assignmentId, 1, "test problem 1", 1, "test problem")
  }

  companion object {
    private val config = loadConfig()

    private val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )

    @JvmStatic
    @AfterAll
    fun reset() {
      reset(database)
    }
  }

  @Test
  fun `test initial state`() {
    val stats = statistics.resolveTeacherStats(teacherId)
    assertTrue(stats.isErr)
    assertEquals(0, statistics.getGlobalStats().totalUncheckedSolutions)
  }

  @Test
  fun `test recording solutions and assessments`() {
    statistics.recordNewSolution(makeSolution(now.minusHours(2)))
    statistics.recordNewSolution(makeSolution(now.minusHours(1)))

    assertEquals(2, statistics.getGlobalStats().totalUncheckedSolutions)

    statistics.recordAssessment(
      teacherId,
      makeSolution(now.minusHours(2)),
      now,
      solutionDistributor,
    )

    val statsResult = statistics.resolveTeacherStats(teacherId)
    assertTrue(statsResult.isOk)
    val stats = statsResult.value
    assertEquals(1, stats.totalAssessments)
    assertEquals(1, statistics.getGlobalStats().totalUncheckedSolutions)
  }

  @Test
  fun `test global statistics`() {
    val now = LocalDateTime.now()
    val sol1 = makeSolution(now.minusHours(3))
    val sol2 = makeSolution(now.minusHours(2))

    statistics.recordNewSolution(sol1)
    statistics.recordNewSolution(sol2)

    statistics.recordAssessment(TeacherId(1L), sol1, now.minusHours(2), solutionDistributor)
    statistics.recordAssessment(TeacherId(2L), sol2, now.minusHours(1), solutionDistributor)

    val globalStats = statistics.getGlobalStats()
    assertEquals(0, globalStats.totalUncheckedSolutions)
    assertTrue(globalStats.averageCheckTimeHours > 0)
  }

  @Test
  fun `teacher gets user solution TEXT`() {
    solutionDistributor.inputSolution(
      studentId,
      RawChatId(0),
      MessageId(0),
      SolutionContent(text = "text"),
      problemId,
    )
    val solution = solutionDistributor.querySolution(teacherId).value!!

    assertEquals(studentId, solution.studentId)
    assertEquals(SolutionContent(text = "text"), solution.content)
    assertEquals(MessageId(0), solution.messageId)
    assertEquals(RawChatId(0), solution.chatId)
  }
}
