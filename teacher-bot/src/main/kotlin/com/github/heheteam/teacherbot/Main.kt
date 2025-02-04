package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.decorators.CoursesDistributorDecorator
import com.github.heheteam.commonlib.decorators.GradeTableDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.teacherbot.run.teacherRun
import org.jetbrains.exposed.sql.Database

/** @param args bot token and telegram @username for mocking data. */
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

  val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val studentStorage = DatabaseStudentStorage(database)

  val googleSheetsService =
    GoogleSheetsService(
      config.googleSheetsConfig.serviceAccountKey,
      config.googleSheetsConfig.spreadsheetId,
    )
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
  val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
  val teacherStatistics = InMemoryTeacherStatistics()

  val botEventBus = RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)

  val solutionResolver =
    SolutionResolver(solutionDistributor, problemStorage, assignmentStorage, studentStorage)
  val solutionAssessor =
    SolutionAssessor(
      teacherStatistics,
      solutionDistributor,
      gradeTable,
      problemStorage,
      botEventBus,
    )
  val coursesStatisticsResolver = CoursesStatisticsResolver(coursesDistributor, gradeTable)

  fillWithSamples(
    coursesDistributor,
    problemStorage,
    assignmentStorage,
    studentStorage,
    teacherStorage,
    database,
  )

  teacherRun(
    botToken,
    teacherStorage,
    teacherStatistics,
    coursesDistributor,
    coursesStatisticsResolver,
    solutionResolver,
    solutionAssessor,
  )
}
