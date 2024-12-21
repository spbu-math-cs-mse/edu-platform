import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudentBotTest {
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var solutionDistributor: SolutionDistributor
  private lateinit var studentCore: StudentCore
  private lateinit var courseIds: List<CourseId>
  private lateinit var gradeTable: GradeTable
  private lateinit var studentStorage: StudentStorage
  private lateinit var problemStorage: ProblemStorage
  private lateinit var assignmentStorage: AssignmentStorage

  private fun createProblem(): ProblemId {
    val courseId = coursesDistributor.createCourse("")
    val assignment =
      assignmentStorage.createAssignment(
        courseId,
        "",
        listOf("1"),
        problemStorage,
      )
    val problemId = problemStorage.getProblemsFromAssignment(assignment).first().id
    return problemId
  }

  private val database =
    Database.connect(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver",
    )

  @BeforeEach
  fun setup() {
    coursesDistributor = DatabaseCoursesDistributor(database)
    solutionDistributor = DatabaseSolutionDistributor(database)
    studentStorage = DatabaseStudentStorage(database)
    assignmentStorage = DatabaseAssignmentStorage(database)
    studentStorage = DatabaseStudentStorage(database)
    problemStorage = DatabaseProblemStorage(database)
    courseIds =
      fillWithSamples(
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        studentStorage,
      )
    gradeTable = DatabaseGradeTable(database)
    studentCore =
      StudentCore(
        solutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        gradeTable,
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

    assertEquals(
      "Вы не записаны ни на один курс!",
      studentCore.getCoursesBulletList(studentId),
    )
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
      val userId = studentStorage.createStudent()

      (0..4).forEach {
        studentCore.inputSolution(
          userId,
          chatId,
          MessageId(it.toLong()),
          SolutionContent(text = "sample$it", type = SolutionType.TEXT),
          createProblem(),
        )
      }

      repeat(5) {
        val solution = solutionDistributor.querySolution(teacherId, gradeTable)
        if (solution != null) {
          solutions.add(solution.id)
          gradeTable.assessSolution(
            solution.id,
            teacherId,
            SolutionAssessment(5, "comment"),
            gradeTable,
            InMemoryTeacherStatistics(),
            LocalDateTime.now(),
          )
        }
      }
    }
    println(solutions)

    val firstSolutionResult = solutionDistributor.resolveSolution(solutions.first())
    assertTrue(firstSolutionResult.isOk)
    val firstSolution = firstSolutionResult.value
    assertEquals(SolutionType.TEXT, firstSolution.type)
    assertEquals("sample0", firstSolution.content.text)

    val lastSolutionResult = solutionDistributor.resolveSolution(solutions.last())
    assertTrue(lastSolutionResult.isOk)
    val lastSolution = lastSolutionResult.value
    assertEquals(SolutionId(5L), lastSolution.id)
    assertEquals(
      solutions
        .map { solutionDistributor.resolveSolution(it).value.chatId }
        .toSet(),
      setOf(chatId),
    )
  }
}
