import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.mock.InMemorySolutionDistributor
import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TeacherBotTest {
  private lateinit var statistics: MockTeacherStatistics
  private val now = LocalDateTime.now()
  private val teacher1Id = 1L
  private val solutionDistributor = InMemorySolutionDistributor()

  private fun makeSolution(timestamp: LocalDateTime) =
    solutionDistributor.inputSolution(
      0L,
      RawChatId(0L),
      MessageId(0L),
      SolutionContent(),
      0L,
    )

  @BeforeEach
  fun setUp() {
    statistics = MockTeacherStatistics()
  }

  @Test
  fun `test initial state`() {
    val stats = statistics.getTeacherStats(teacher1Id)
    assertNull(stats)
    assertEquals(0, statistics.getGlobalStats().totalUncheckedSolutions)
  }

  @Test
  fun `test recording solutions and assessments`() {
    statistics.recordNewSolution(makeSolution(now.minusHours(2)))
    statistics.recordNewSolution(makeSolution(now.minusHours(1)))

    assertEquals(2, statistics.getGlobalStats().totalUncheckedSolutions)

    statistics.recordAssessment(
      teacher1Id,
      makeSolution(now.minusHours(2)),
      now,
      solutionDistributor,
    )

    val stats = statistics.getTeacherStats(teacher1Id)
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
      teacher1Id,
      sol1,
      now.minusHours(4),
      solutionDistributor,
    )
    statistics.recordAssessment(
      teacher1Id,
      sol2,
      now.minusHours(1),
      solutionDistributor,
    )
    statistics.recordAssessment(teacher1Id, sol3, now, solutionDistributor)

    val stats = statistics.getTeacherStats(teacher1Id)
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
      1L,
      sol1,
      now.minusHours(2),
      solutionDistributor,
    )
    statistics.recordAssessment(
      2L,
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
    val studentId = 10L
    val teacherId = 139L
    val inMemorySolutionDistributor = InMemorySolutionDistributor()
    inMemorySolutionDistributor.inputSolution(
      studentId,
      RawChatId(0),
      MessageId(0),
      SolutionContent(text = "test"),
      0L,
    )
    val solution = inMemorySolutionDistributor.resolveSolution(
      inMemorySolutionDistributor.querySolution(teacherId)!!,
    )
    assertEquals(studentId, solution.studentId)
    assertEquals(SolutionContent(text = "test"), solution.content)
    assertEquals(SolutionType.TEXT, solution.type)
    assertEquals(MessageId(0), solution.messageId)
    assertEquals(RawChatId(0), solution.chatId)
  }
}
