package com.github.heheteam.studentbot

import DatabaseCoursesDistributor
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.studentbot.run.studentRun
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import org.jetbrains.exposed.sql.Database

suspend fun main(vararg args: String) {
  val botToken = args.first()

  val database = Database.connect(
    "jdbc:h2:./data/films",
    driver = "org.h2.Driver",
  )

  val studentStorage = DatabaseStudentStorage(database)
  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor = DatabaseSolutionDistributor(database)

  val bot = telegramBot(botToken) {
    logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
      println(defaultMessageFormatter(level, tag, message, throwable))
    }
  }
  val notificationService = StudentNotificationService(bot)
  val botEventBus = RedisBotEventBus()

  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage)

  val userIdRegistry = MockStudentIdRegistry(1L)

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

  studentRun(botToken, userIdRegistry, studentStorage, core)
}
