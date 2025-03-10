package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.decorators.AssignmentStorageDecorator
import com.github.heheteam.commonlib.decorators.CoursesDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.util.fillWithSamples
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

  val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val scheduledMessagesDistributor: ScheduledMessagesDistributor =
    InMemoryScheduledMessagesDistributor()
  val studentStorage = DatabaseStudentStorage(database)

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
  val assignmentStorageDecorator = AssignmentStorageDecorator(assignmentStorage, ratingRecorder)
  val coursesDistributor = CoursesDistributorDecorator(databaseCoursesDistributor, ratingRecorder)

  fillWithSamples(
    coursesDistributor,
    problemStorage,
    assignmentStorageDecorator,
    studentStorage,
    teacherStorage,
    database,
  )

  val core =
    AdminCore(scheduledMessagesDistributor, coursesDistributor, studentStorage, teacherStorage)

  adminRun(
    botToken,
    coursesDistributor,
    assignmentStorageDecorator,
    problemStorage,
    solutionDistributor,
    core,
  )
}
