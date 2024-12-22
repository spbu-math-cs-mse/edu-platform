package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.facades.CoursesDistributorFacade
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
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

  val database = Database.connect(
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

  val googleSheetsService =
    GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey, config.googleSheetsConfig.spreadsheetId)
  val ratingRecorder = GoogleSheetsRatingRecorder(
    googleSheetsService,
    databaseCoursesDistributor,
    assignmentStorage,
    problemStorage,
    databaseGradeTable,
    solutionDistributor,
  )
  val coursesDistributor = CoursesDistributorFacade(databaseCoursesDistributor, ratingRecorder)
  val botEventBus = RedisBotEventBus()

  val bot = telegramBot(botToken) {
    logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
      println(defaultMessageFormatter(level, tag, message, throwable))
    }
  }

  val notificationService = StudentNotificationService(bot)

  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage, database)

  val core =
    StudentCore(
      solutionDistributor,
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      DatabaseGradeTable(database),
      notificationService,
      botEventBus,
      InMemoryScheduledMessagesDistributor(),
      studentStorage,
    )

  studentRun(botToken, studentStorage, core)
}
