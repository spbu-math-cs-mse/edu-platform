import com.github.heheteam.adminbot.*
import com.github.heheteam.adminbot.mockCoursesTable
import com.github.heheteam.adminbot.mockStudentsTable
import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.mock.MockCoursesDistributor
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminBotTest {
  private val core =
    AdminCore(
      mockGradeTable,
      InMemoryScheduledMessagesDistributor(),
      mockCoursesTable,
      mockStudentsTable,
      mockTeachersTable,
      mockAdminsTable,
      MockCoursesDistributor(),
    )

  private val student = Student(1L)
  private val teacher = Teacher(1L)
  private val course =
    Course(
      1L,
      "",
    )

  @Test
  fun scheduledMessagesDistributorTest() {
    val date1 = LocalDateTime.now()
    val date2 = date1.plusDays(1)
    val message1 = ScheduledMessage(course, date1.minusHours(1), "message 1")
    val message2 = ScheduledMessage(course, date2.minusHours(1), "message 2")

    assertEquals(listOf(), core.getMessagesUpToDate(date1))
    assertEquals(listOf(), core.getMessagesUpToDate(date2))
    core.addMessage(message1)
    core.addMessage(message2)
    assertEquals(listOf(message1), core.getMessagesUpToDate(date1))
    assertEquals(listOf(message1, message2), core.getMessagesUpToDate(date2))
    core.markMessagesUpToDateAsSent(date1)
    assertEquals(listOf(), core.getMessagesUpToDate(date1))
    assertEquals(listOf(message2), core.getMessagesUpToDate(date2))
  }

  @Test
  fun coursesTableTest() {
    val courseName = "course 1"

    assertEquals(false, core.courseExists(courseName))
    assertEquals(null, core.getCourse(courseName))
    assertEquals(mockCoursesTable.toMap(), core.getCourses())
    core.addCourse(courseName, course)
    assertEquals(true, core.courseExists(courseName))
    assertEquals(course, core.getCourse(courseName))
    assertEquals(mockCoursesTable.toMap().plus(courseName to course), core.getCourses())
  }
}
