package com.github.heheteam.multibot

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.facades.CoursesDistributorFacade
import com.github.heheteam.commonlib.facades.GradeTableFacade
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.parentbot.run.parentRun
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.run.studentRun
import com.github.heheteam.teacherbot.TeacherCore
import com.github.heheteam.teacherbot.run.teacherRun
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
  val inMemoryTeacherStatistics: TeacherStatistics = InMemoryTeacherStatistics()
  val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
    InMemoryScheduledMessagesDistributor()
  val studentStorage = DatabaseStudentStorage(database)
  fillWithSamples(databaseCoursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage, database)

  val parentStorage = MockParentStorage()

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
  val gradeTable = GradeTableFacade(databaseGradeTable, ratingRecorder)

  val studentIdRegistry = MockStudentIdRegistry(1L)
  val studentCore =
    StudentCore(
      solutionDistributor,
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      gradeTable,
    )

  val teacherIdRegistry = MockTeacherIdRegistry(1L)
  val teacherCore =
    TeacherCore(
      inMemoryTeacherStatistics,
      coursesDistributor,
      solutionDistributor,
      gradeTable,
    )

  val adminIdRegistry = MockAdminIdRegistry(1L)
  val adminCore =
    AdminCore(
      inMemoryScheduledMessagesDistributor,
      coursesDistributor,
      studentStorage,
      teacherStorage,
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
