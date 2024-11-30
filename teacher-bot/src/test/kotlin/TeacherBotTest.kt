import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TeacherBotTest {
  private lateinit var statistics: InMemoryTeacherStatistics
  private val now = LocalDateTime.now()
  private lateinit var teacherId: TeacherId
  private lateinit var studentId: StudentId
  private lateinit var problemId: ProblemId
  val database =
    Database.connect(
      "jdbc:h2:./data/films",
      driver = "org.h2.Driver",
    )
  private val solutionDistributor = DatabaseSolutionDistributor(database)
  private val teacherStorage = DatabaseTeacherStorage(database)

  private fun makeSolution(timestamp: LocalDateTime) =
    solutionDistributor.inputSolution(
      studentId,
      RawChatId(0L),
      MessageId(0L),
      SolutionContent(),
      problemId,
      timestamp,
    )

  @BeforeEach
  fun setUp() {
    statistics = InMemoryTeacherStatistics()
    reset(database)
    teacherId = teacherStorage.createTeacher()
    studentId = DatabaseStudentStorage(database).createStudent()
    val courseId = DatabaseCoursesDistributor(database).createCourse("test course")
    val assignmentId =
      DatabaseAssignmentStorage(
        database,
      ).createAssignment(courseId, "test assignment", listOf("p1", "p2"), DatabaseProblemStorage(database))
    problemId = DatabaseProblemStorage(database).createProblem(assignmentId, "test problem")
  }

  @Test
  fun `test initial state`() {
    val stats = statistics.getTeacherStats(teacherId)
    assertNull(stats)
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

    val stats = statistics.getTeacherStats(teacherId)
    assertEquals(1, stats!!.totalAssessments)
    assertEquals(1, statistics.getGlobalStats().totalUncheckedSolutions)
  }

  @Test
  fun `test average check time calculation`() {
    val sol1 = makeSolution(now.minusHours(6))
    val sol2 = makeSolution(now.minusHours(2))
    val sol3 = makeSolution(now)
    statistics.recordNewSolution(sol1)
    statistics.recordNewSolution(sol2)
    statistics.recordNewSolution(sol3)
    statistics.recordAssessment(
      teacherId,
      sol1,
      now.minusHours(4),
      solutionDistributor,
    )
    statistics.recordAssessment(
      teacherId,
      sol2,
      now.minusHours(1),
      solutionDistributor,
    )
    statistics.recordAssessment(teacherId, sol3, now, solutionDistributor)

    val stats = statistics.getTeacherStats(teacherId)
    assertEquals(1.0 * 60 * 60, stats!!.averageCheckTimeSeconds, 0.01)
  }

  @Test
  fun `test global statistics`() {
    val now = LocalDateTime.now()
    val sol1 = makeSolution(now.minusHours(3))
    val sol2 = makeSolution(now.minusHours(2))

    statistics.recordNewSolution(sol1)
    statistics.recordNewSolution(sol2)

    statistics.recordAssessment(
      TeacherId(1L),
      sol1,
      now.minusHours(2),
      solutionDistributor,
    )
    statistics.recordAssessment(
      TeacherId(2L),
      sol2,
      now.minusHours(1),
      solutionDistributor,
    )

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
      SolutionContent(text = "test"),
      problemId,
    )
    val solution =
      solutionDistributor.resolveSolution(
        solutionDistributor.querySolution(teacherId, DatabaseGradeTable(database))!!,
      )
    assertEquals(studentId, solution.studentId)
    assertEquals(SolutionContent(listOf(), text = "test"), solution.content)
    assertEquals(SolutionType.TEXT, solution.type)
    assertEquals(MessageId(0), solution.messageId)
    assertEquals(RawChatId(0), solution.chatId)
  }
}
