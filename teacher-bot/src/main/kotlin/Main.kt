package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.facades.CoursesDistributorDecorator
import com.github.heheteam.commonlib.facades.GradeTableDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.teacherbot.run.teacherRun
import org.jetbrains.exposed.sql.Database

/**
 * @param args bot token and telegram @username for mocking data.
 */
suspend fun main(vararg args: String) {
  val botToken = args.first()
  val config = loadConfig()

  val database = Database.connect(
    config.databaseConfig.url,
    config.databaseConfig.driver,
    config.databaseConfig.login,
    config.databaseConfig.password,
  )

  val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val studentStorage = DatabaseStudentStorage(database)

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
  val coursesDistributor = CoursesDistributorDecorator(databaseCoursesDistributor, ratingRecorder)
  val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
  val teacherStatistics = InMemoryTeacherStatistics()
  val botEventBus = RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)

  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage, database)

  val core =
    TeacherCore(
      teacherStatistics,
      coursesDistributor,
      solutionDistributor,
      gradeTable,
      problemStorage,
      botEventBus,
      assignmentStorage,
      studentStorage,
    )

  teacherRun(botToken, teacherStorage, core)
}
