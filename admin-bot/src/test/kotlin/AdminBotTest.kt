import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ScheduledMessage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.googlesheets.MockRatingRecorder
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import org.jetbrains.exposed.sql.Database
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminBotTest {
  private val config = loadConfig()

  private val database = Database.connect(
    config.databaseConfig.url,
    config.databaseConfig.driver,
    config.databaseConfig.login,
    config.databaseConfig.password,
  )

  private val core =
    AdminCore(
      InMemoryScheduledMessagesDistributor(),
      DatabaseCoursesDistributor(database),
      DatabaseStudentStorage(database),
      DatabaseTeacherStorage(database),
      MockRatingRecorder(),
    )

  private val course =
    Course(
      CourseId(1L),
      "",
    )

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
  }

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
    assertEquals(mapOf(), core.getCourses())

    core.addCourse(courseName)
    assertEquals(true, core.courseExists(courseName))
    assertEquals(Course(CourseId(1), courseName), core.getCourse(courseName))
    assertEquals(mapOf(courseName to Course(CourseId(1), courseName)), core.getCourses())
  }
}
