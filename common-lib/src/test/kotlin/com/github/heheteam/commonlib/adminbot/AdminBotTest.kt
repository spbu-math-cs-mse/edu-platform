package com.github.heheteam.commonlib.adminbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.database.DatabaseAdminStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseScheduledMessagesDistributor
import com.github.heheteam.commonlib.database.DatabaseSentMessageLogStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseTokenStorage
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.Database

class AdminBotTest {
  private val config = loadConfig()

  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )
  private val core: AdminApi
  private val studentBotController = mockk<StudentBotTelegramController>(relaxed = true)

  init {
    val problemStorage = DatabaseProblemStorage(database)
    val sentMessageLogStorage = DatabaseSentMessageLogStorage(database)
    core =
      AdminApi(
        DatabaseScheduledMessagesDistributor(database, sentMessageLogStorage, studentBotController),
        DatabaseCourseStorage(database),
        DatabaseAdminStorage(database),
        DatabaseStudentStorage(database),
        DatabaseTeacherStorage(database),
        DatabaseAssignmentStorage(database, problemStorage),
        problemStorage,
        DatabaseSubmissionDistributor(database),
        mockk<PersonalDeadlinesService>(relaxed = true),
        mockk<CourseTokenStorage>(relaxed = true),
      )
  }

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
  }

  @Test
  fun coursesTableTest() {
    val courseName = "course 1"
    assertEquals(false, core.courseExists(courseName))
    assertEquals(null, core.getCourse(courseName).value)
    assertEquals(mapOf(), core.getCourses().value)

    core.createCourse(courseName)
    assertEquals(true, core.courseExists(courseName))
    assertEquals(Course(CourseId(1), courseName), core.getCourse(courseName).value)
    assertEquals(mapOf(courseName to Course(CourseId(1), courseName)), core.getCourses().value)
  }
}
