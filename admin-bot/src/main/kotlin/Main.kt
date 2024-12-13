package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.MockAdminIdRegistry
import dev.inmo.tgbotapi.utils.RiskFeature
import org.jetbrains.exposed.sql.Database

@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
  val botToken = args.first()
  val config = loadConfig()

  val database = Database.connect(
    config.databaseConfig.url,
    config.databaseConfig.driver,
    config.databaseConfig.login,
    config.databaseConfig.password,
  )

  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val gradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val scheduledMessagesDistributor: ScheduledMessagesDistributor = InMemoryScheduledMessagesDistributor()
  val studentStorage = DatabaseStudentStorage(database)

  val googleSheetsService =
    GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey, config.googleSheetsConfig.spreadsheetId)
  val ratingRecorder = GoogleSheetsRatingRecorder(
    googleSheetsService,
    coursesDistributor,
    assignmentStorage,
    problemStorage,
    gradeTable,
    solutionDistributor,
  )

  val userIdRegistry = MockAdminIdRegistry(0L)
  val core =
    AdminCore(
      scheduledMessagesDistributor,
      coursesDistributor,
      studentStorage,
      teacherStorage,
      ratingRecorder,
    )

  adminRun(botToken, userIdRegistry, core)
}
