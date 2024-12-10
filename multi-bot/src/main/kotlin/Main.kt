package com.github.heheteam.multibot

import DatabaseCoursesDistributor
import GoogleSheetsService
import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.GoogleSheetsConfig
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.parentbot.run.parentRun
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.run.studentRun
import com.github.heheteam.teacherbot.TeacherCore
import com.github.heheteam.teacherbot.run.teacherRun
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import java.io.File

// this is a sample multi-bot running main
/**
 * @param args tokens for bots in the FOLLOWING order: student, teacher, admin, parent
 */
fun main(vararg args: String) {
  val dbFile = File("./data/edu-platform.mv.db")
  if (dbFile.exists()) {
    dbFile.delete()
  }

  val database = Database.connect(
    "jdbc:h2:./data/edu-platform",
    driver = "org.h2.Driver",
  )

  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val gradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val inMemoryTeacherStatistics: TeacherStatistics = InMemoryTeacherStatistics()
  val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
    InMemoryScheduledMessagesDistributor()
  val studentStorage = DatabaseStudentStorage(database)
  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage)

  val parentStorage = MockParentStorage()

  val config = ConfigLoaderBuilder
    .default()
    .addResourceSource("/google_sheets.yaml")
    .build()
    .loadConfigOrThrow<GoogleSheetsConfig>()
  val googleSheetsService = GoogleSheetsService(config.serviceAccountKey, config.spreadsheetId)
  val ratingRecorder = GoogleSheetsRatingRecorder(googleSheetsService, coursesDistributor, assignmentStorage, problemStorage, gradeTable, solutionDistributor)

  val studentIdRegistry = MockStudentIdRegistry(1L)
  val studentCore =
    StudentCore(
      solutionDistributor,
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      gradeTable,
      ratingRecorder,
    )

  val teacherIdRegistry = MockTeacherIdRegistry(1L)
  val teacherCore =
    TeacherCore(
      inMemoryTeacherStatistics,
      coursesDistributor,
      solutionDistributor,
      gradeTable,
      ratingRecorder,
    )

  val adminIdRegistry = MockAdminIdRegistry(1L)
  val adminCore =
    AdminCore(
      inMemoryScheduledMessagesDistributor,
      coursesDistributor,
      studentStorage,
      teacherStorage,
      ratingRecorder,
    )

  val parentIdRegistry = MockParentIdRegistry(1L)
  val parentCore =
    ParentCore(
      DatabaseStudentStorage(database),
      DatabaseGradeTable(database),
      DatabaseSolutionDistributor(database),
    )

  runBlocking {
    launch { studentRun(args[0], studentIdRegistry, studentStorage, studentCore) }
    launch { teacherRun(args[1], teacherIdRegistry, teacherStorage, teacherCore) }
    launch { adminRun(args[2], adminIdRegistry, adminCore) }
    launch { parentRun(args[3], parentIdRegistry, parentStorage, parentCore) }
  }
}
