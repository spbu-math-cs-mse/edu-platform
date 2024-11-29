import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.mock.MockTeacherStatistics
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class StudentBotTest {
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var inMemorySolutionDistributor: InMemorySolutionDistributor
  private lateinit var studentCore: StudentCore
  private lateinit var courseIds: List<CourseId>

  @BeforeEach
  fun setup() {
    val database = Database.connect(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver",
    )
    transaction(database) {
      exec("SET REFERENTIAL_INTEGRITY FALSE;")
    }
    val problemStorage = InMemoryProblemStorage()
    val assignmentStorage = InMemoryAssignmentStorage()
    DatabaseStudentStorage(database)
    coursesDistributor = DatabaseCoursesDistributor(database)
    inMemorySolutionDistributor = InMemorySolutionDistributor()
    courseIds = fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, InMemoryStudentStorage())
    studentCore =
      StudentCore(
        inMemorySolutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        InMemoryGradeTable(),
      )
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = StudentId(25L)

    val studentCourses = studentCore.getStudentCourses(studentId)
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.id })

    assertEquals(
      "Вы не записаны ни на один курс!",
      studentCore.getCoursesBulletList(studentId),
    )
  }

  @Test
  fun `new student courses handling test`() {
    val studentId = StudentId(36L)

    studentCore.addRecord(studentId, courseIds[0])
    studentCore.addRecord(studentId, courseIds[3])

    val studentCourses = studentCore.getStudentCourses(studentId)

    assertEquals(
      listOf(courseIds[0], courseIds[3]),
      studentCourses.map { it.id }.sortedBy { it.id },
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
      val teacherId = TeacherId(0L)
      val userId = StudentId(0L)

      (0..4).forEach {
        studentCore.inputSolution(
          userId,
          chatId,
          MessageId(it.toLong()),
          SolutionContent(text = "sample$it"),
          ProblemId(0L),
        )
      }

      val gradeTable = InMemoryGradeTable()

      repeat(5) {
        val solution = inMemorySolutionDistributor.querySolution(teacherId)
        if (solution != null) {
          solutions.add(solution)
          inMemorySolutionDistributor.assessSolution(
            solution,
            teacherId,
            SolutionAssessment(5, "comment"),
            gradeTable,
            MockTeacherStatistics(),
            LocalDateTime.now(),
          )
        }
      }
    }
    println(solutions)

    val firstSolution = inMemorySolutionDistributor.resolveSolution(solutions.first())
    assertEquals(SolutionType.TEXT, firstSolution.type)
    assertEquals("sample0", firstSolution.content.text)

    val lastSolution = inMemorySolutionDistributor.resolveSolution(solutions.last())
    assertEquals(SolutionId(5L), lastSolution.id)

    assertEquals(
      solutions
        .map { inMemorySolutionDistributor.resolveSolution(it).chatId }
        .toSet(),
      setOf(chatId),
    )
  }
}
