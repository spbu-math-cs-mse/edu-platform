package com.github.heheteam.multibot

import DatabaseCoursesDistributor
import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.mock.MockAdminIdRegistry
import com.github.heheteam.commonlib.mock.MockParentIdRegistry
import com.github.heheteam.commonlib.mock.MockStudentIdRegistry
import com.github.heheteam.commonlib.mock.MockTeacherIdRegistry
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
  val dbFile = File("./data/films.mv.db")
  if (dbFile.exists()) {
    dbFile.delete()
  }

  val database = Database.connect(
    "jdbc:h2:./data/films",
    driver = "org.h2.Driver",
  )

  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
  val gradeTable: GradeTable = DatabaseGradeTable(database)
  val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
  val inMemoryTeacherStatistics: TeacherStatistics = InMemoryTeacherStatistics()
  val inMemoryScheduledMessagesDistributor: InMemoryScheduledMessagesDistributor = InMemoryScheduledMessagesDistributor()

  val studentStorage = DatabaseStudentStorage(database)
  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage)

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
    launch { studentRun(args[0], studentIdRegistry, studentCore) }
    launch { teacherRun(args[1], teacherIdRegistry, teacherCore) }
    launch { adminRun(args[2], adminIdRegistry, adminCore) }
    launch { parentRun(args[3], parentIdRegistry, parentCore) }
  }
}
