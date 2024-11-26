import com.github.heheteam.commonlib.MockCoursesDistributor
import com.github.heheteam.commonlib.MockGradeTable
import com.github.heheteam.commonlib.MockSolutionDistributor
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class StudentBotTest {
  private lateinit var mockCoursesDistributor: MockCoursesDistributor
  private lateinit var mockSolutionDistributor: MockSolutionDistributor
  private lateinit var studentCore: StudentCore

  @BeforeEach
  fun setup() {
    mockCoursesDistributor = MockCoursesDistributor()
    mockSolutionDistributor = MockSolutionDistributor()
    studentCore =
      StudentCore(
        mockSolutionDistributor,
        mockCoursesDistributor,
      )
  }

  @Test
  fun `testing checking grades`() {
    val userId = mockCoursesDistributor.singleUserId

    run {
      val firstCourse = studentCore.getStudentCourses(userId).first()
      val firstAssignment = firstCourse.assignments.first()
      (firstCourse.gradeTable as MockGradeTable).addMockFilling(
        firstAssignment,
        userId,
      )
    }
    // check first input correctness
    val availableCourses = studentCore.getStudentCourses(userId)
    assert(availableCourses.any { it.id == "0" })
    val course = mockCoursesDistributor.getStudentCourses("0").first()
    val assignment = course.assignments.first()
    val grading =
      studentCore.getGradingForAssignment(
        assignment,
        course,
        userId,
      )
    // check output correctness
    assertEquals(listOf(null, 1, null, 0), grading.map { (_, grade) -> grade })
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = "rA9"

    val availableCourses = studentCore.getAvailableCourses(studentId)
    assertEquals(4, availableCourses.size)
    assertEquals((1..4).map { true }.toList(), availableCourses.map { it.second }.toList())

    val studentCourses = studentCore.getStudentCourses(studentId)
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.toInt() })

    assertEquals("Вы не записаны ни на один курс!", studentCore.getCoursesBulletList(studentId))
  }

  @Test
  fun `new student courses handling test`() {
    val studentId = "rA9"

    run {
      studentCore.addRecord(studentId, "0")
      studentCore.addRecord(studentId, "3")
    }

    val studentCourses = studentCore.getStudentCourses(studentId)

    assertEquals(listOf("0", "3"), studentCourses.map { it.id }.sortedBy { it.toInt() })
    assertEquals("Начала мат. анализа", studentCourses.first().description)

    assertEquals("- Начала мат. анализа\n- ТФКП", studentCore.getCoursesBulletList(studentId))
  }

  @Test
  fun `send solution test`() {
    val solutions = mutableListOf<Solution>()
    val chatId = RawChatId(0)

    run {
      val teacherId = "0"
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
          )
        }
      }
    }

    val firstSolution = solutions.first()
    assertEquals(SolutionType.TEXT, firstSolution.type)
    assertEquals("sample0", firstSolution.content.text)

    val lastSolution = solutions.last()
    assertEquals("5", lastSolution.id)

    assertEquals(solutions.map { it.chatId }.toSet(), setOf(chatId))
  }
}
