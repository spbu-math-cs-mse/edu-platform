package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.facades.CoursesDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.util.fillWithSamples
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

  val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val scheduledMessagesDistributor: ScheduledMessagesDistributor = InMemoryScheduledMessagesDistributor()
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

  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage, database)

  val core =
    AdminCore(
      scheduledMessagesDistributor,
      coursesDistributor,
      studentStorage,
      teacherStorage,
      assignmentStorage,
      problemStorage,
      solutionDistributor,
    )

  adminRun(botToken, core)
}
