package com.github.heheteam.studentbot

import DatabaseCoursesDistributor
import GoogleSheetsService
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.MockStudentIdRegistry
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.studentbot.run.studentRun
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
  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor = DatabaseSolutionDistributor(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val gradeTable = DatabaseGradeTable(database)

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
  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage, database)

  val userIdRegistry = MockStudentIdRegistry(1L)

  val core =
    StudentCore(
      solutionDistributor,
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      DatabaseGradeTable(database),
      ratingRecorder,
    )

  studentRun(botToken, userIdRegistry, studentStorage, core)
}
