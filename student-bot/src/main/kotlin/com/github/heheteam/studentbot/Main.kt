package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.StudentNotificationService
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.decorators.CoursesDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.studentbot.run.studentRun
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import org.jetbrains.exposed.sql.Database

suspend fun main(vararg args: String) {
  val botToken = args.first()
  val config = loadConfig()

  val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  val studentStorage = DatabaseStudentStorage(database)
  val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor = DatabaseSolutionDistributor(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val databaseGradeTable = DatabaseGradeTable(database)

  val googleSheetsService = GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey)
  val ratingRecorder =
    GoogleSheetsRatingRecorder(
      googleSheetsService,
      databaseCoursesDistributor,
      assignmentStorage,
      problemStorage,
      databaseGradeTable,
      solutionDistributor,
    )
  val coursesDistributor = CoursesDistributorDecorator(databaseCoursesDistributor, ratingRecorder)
  val botEventBus = RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)

  val bot =
    telegramBot(botToken) {
      logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
        println(defaultMessageFormatter(level, tag, message, throwable))
      }
    }

  val notificationService = StudentNotificationService(bot)

  fillWithSamples(
    coursesDistributor,
    problemStorage,
    assignmentStorage,
    studentStorage,
    teacherStorage,
    database,
  )

  val core =
    StudentCore(
      solutionDistributor,
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      DatabaseGradeTable(database),
      notificationService,
      botEventBus,
    )

  studentRun(botToken, studentStorage, coursesDistributor, problemStorage, core)
}
