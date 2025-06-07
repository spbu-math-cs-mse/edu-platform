package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.database.DatabaseAdminStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.util.fillWithSamples
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseStartupTest {
  private lateinit var database: Database
  private lateinit var assignmentStorage: AssignmentStorage
  private lateinit var courseStorage: CourseStorage
  private lateinit var gradeTable: GradeTable
  private lateinit var problemStorage: ProblemStorage
  private lateinit var submissionDistributor: SubmissionDistributor
  private lateinit var adminStorage: AdminStorage
  private lateinit var studentStorage: StudentStorage
  private lateinit var teacherStorage: TeacherStorage

  @Test
  fun startupTest() {
    val startupTime = measureTimeMillis {
      val config = loadConfig()
      database =
        Database.connect(
          config.databaseConfig.url,
          config.databaseConfig.driver,
          config.databaseConfig.login,
          config.databaseConfig.password,
        )

      problemStorage = DatabaseProblemStorage(database)
      assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
      courseStorage = DatabaseCourseStorage(database)
      gradeTable = DatabaseGradeTable(database)
      submissionDistributor = DatabaseSubmissionDistributor(database)
      adminStorage = DatabaseAdminStorage(database)
      studentStorage = DatabaseStudentStorage(database)
      teacherStorage = DatabaseTeacherStorage(database)

      fillWithSamples(
        courseStorage,
        assignmentStorage,
        adminStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    }
    println("Startup time: ${startupTime.toFloat() / 1000.0} s")
    assertTrue(startupTime < 12000)

    val transactionsTime = measureTimeMillis {
      transaction {
        val course = courseStorage.getCourses().value.first()
        courseStorage.getStudents(course.id)
        courseStorage.getTeachers(course.id)
        problemStorage.getProblemsFromCourse(course.id)
        assignmentStorage.getAssignmentsForCourse(course.id)
      }
    }
    println("Transactions time: ${transactionsTime.toFloat() / 1000.0} s")
    assertTrue(transactionsTime < 300)
  }
}
