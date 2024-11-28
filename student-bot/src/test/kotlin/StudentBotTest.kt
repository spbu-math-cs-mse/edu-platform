import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class StudentBotTest {
  private lateinit var mockCoursesDistributor: MockCoursesDistributor
  private lateinit var mockSolutionDistributor: InMemorySolutionDistributor
  private lateinit var studentCore: StudentCore

  @BeforeEach
  fun setup() {
    val problemStorage = InMemoryProblemStorage()
    val assignmentStorage = InMemoryAssignmentStorage()
    mockCoursesDistributor = MockCoursesDistributor()
    mockSolutionDistributor = InMemorySolutionDistributor()
    studentCore =
      StudentCore(
        mockSolutionDistributor,
        mockCoursesDistributor,
        problemStorage,
        assignmentStorage,
      )
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = 25L

    val studentCourses = studentCore.getStudentCourses(studentId)
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.toInt() })

    assertEquals(
      "Вы не записаны ни на один курс!",
      studentCore.getCoursesBulletList(studentId),
    )
  }

  @Test
  fun `new student courses handling test`() {
    val studentId = 36L

    run {
      studentCore.addRecord(studentId, 0L)
      studentCore.addRecord(studentId, 3L)
    }

    val studentCourses = studentCore.getStudentCourses(studentId)

    assertEquals(
      listOf(0L, 3L),
      studentCourses.map { it.id }.sortedBy { it.toInt() },
    )
    assertEquals("Начала мат. анализа", studentCourses.first().description)

    assertEquals(
      "- Начала мат. анализа\n- ТФКП",
      studentCore.getCoursesBulletList(studentId),
    )
  }

  @Test
  fun `send solution test`() {
    val solutions = mutableListOf<SolutionId>()
    val chatId = RawChatId(0)

    run {
      val teacherId = 0L
      val userId = mockCoursesDistributor.singleUserId

      (0..4).forEach {
        studentCore.inputSolution(
          userId,
          chatId,
          MessageId(it.toLong()),
          SolutionContent(text = "sample$it"),
        )
      }

      val gradeTable = MockGradeTable()

      repeat(5) {
        val solution = mockSolutionDistributor.querySolution(teacherId)
        if (solution != null) {
          solutions.add(solution)
          mockSolutionDistributor.assessSolution(
            solution,
            teacherId,
            SolutionAssessment(5, "comment"),
            gradeTable,
            LocalDateTime.now(),
            MockTeacherStatistics(),
          )
        }
      }
    }

    val firstSolution =
      mockSolutionDistributor.resolveSolution(solutions.first())
    assertEquals(SolutionType.TEXT, firstSolution.type)
    assertEquals("sample0", firstSolution.content.text)

    val lastSolution = mockSolutionDistributor.resolveSolution(solutions.last())
    assertEquals(5L, lastSolution.id)

    assertEquals(
      solutions.map { mockSolutionDistributor.resolveSolution(it).chatId }
        .toSet(),
      setOf(chatId),
    )
  }
}
