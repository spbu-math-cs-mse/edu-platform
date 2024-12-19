package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.parentbot.run.parentRun
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.run.studentRun
import com.github.heheteam.teacherbot.TeacherCore
import com.github.heheteam.teacherbot.run.teacherRun
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

class MultiBotRunner : CliktCommand() {
  val studentBotToken: String by option().required().help("student bot token")
  val teacherBotToken: String by option().required().help("teacher bot token")
  val adminBotToken: String by option().required().help("admin bot token")
  val parentBotToken: String by option().required().help("parent bot token")
  val presetStudentId: Long? by option().long()
  val presetTeacherId: Long? by option().long()

  override fun run() {
    val config = loadConfig()
    val database = Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

    val coursesDistributor = DatabaseCoursesDistributor(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val assignmentStorage: AssignmentStorage =
      DatabaseAssignmentStorage(database)
    val solutionDistributor: SolutionDistributor =
      DatabaseSolutionDistributor(database)
    val gradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
    val inMemoryTeacherStatistics: TeacherStatistics =
      InMemoryTeacherStatistics()
    val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
      InMemoryScheduledMessagesDistributor()

    val studentStorage = DatabaseStudentStorage(database)
    fillWithSamples(
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      studentStorage,
      teacherStorage,
      database,
    )

    val parentStorage = MockParentStorage()

    val bot = telegramBot(studentBotToken) {
      logger =
        KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
          println(defaultMessageFormatter(level, tag, message, throwable))
        }
    }
    val notificationService = StudentNotificationService(bot)
    val botEventBus = RedisBotEventBus()
    val studentCore =
      StudentCore(
        solutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        gradeTable,
        notificationService,
        botEventBus,
      )

    val teacherCore =
      TeacherCore(
        inMemoryTeacherStatistics,
        coursesDistributor,
        solutionDistributor,
        gradeTable,
        problemStorage,
        botEventBus,
      )

    val adminCore =
      AdminCore(
        inMemoryScheduledMessagesDistributor,
        coursesDistributor,
        studentStorage,
        teacherStorage,
        assignmentStorage,
        problemStorage,
      )

    val parentCore =
      ParentCore(
        DatabaseStudentStorage(database),
        DatabaseGradeTable(database),
        DatabaseSolutionDistributor(database),
      )
    val presetStudent = presetStudentId?.toStudentId()
    val presetTeacher = presetTeacherId?.toTeacherId()
    val developerOptions = DeveloperOptions(presetStudent, presetTeacher)
    runBlocking {
      launch {
        studentRun(
          studentBotToken,
          studentStorage,
          studentCore,
          developerOptions,
        )
      }
      launch {
        teacherRun(
          teacherBotToken,
          teacherStorage,
          teacherCore,
          developerOptions,
        )
      }
      launch { adminRun(adminBotToken, adminCore) }
      launch { parentRun(parentBotToken, parentStorage, parentCore) }
    }
  }
}
