package com.github.heheteam.commonlib.adminbot

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.config.loadConfig
import com.github.heheteam.commonlib.database.DatabaseAdminStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseRepository
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseScheduledMessagesStorage
import com.github.heheteam.commonlib.database.DatabaseSentMessageLogStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.logic.AdminAuthService
import com.github.heheteam.commonlib.logic.CourseTokenService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageService
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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
    val courseStorage = DatabaseCourseStorage(DatabaseCourseRepository())
    core =
      AdminApi(
        ScheduledMessageService(
          DatabaseScheduledMessagesStorage(database),
          sentMessageLogStorage,
          courseStorage,
          studentBotController,
        ),
        courseStorage,
        AdminAuthService(DatabaseAdminStorage(database)),
        DatabaseStudentStorage(database),
        DatabaseTeacherStorage(database),
        DatabaseAssignmentStorage(database, problemStorage),
        problemStorage,
        DatabaseSubmissionDistributor(database),
        mockk<PersonalDeadlinesService>(relaxed = true),
        mockk<CourseTokenService>(relaxed = true),
        ErrorManagementService(),
      )
  }

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
  }
}
