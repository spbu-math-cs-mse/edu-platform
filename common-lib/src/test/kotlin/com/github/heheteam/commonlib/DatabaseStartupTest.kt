package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.util.fillWithSamples
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseStartupTest {
  private lateinit var database: Database
  private lateinit var assignmentStorage: AssignmentStorage
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var gradeTable: GradeTable
  private lateinit var problemStorage: ProblemStorage
  private lateinit var solutionDistributor: SolutionDistributor
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

      assignmentStorage = DatabaseAssignmentStorage(database)
      coursesDistributor = DatabaseCoursesDistributor(database)
      gradeTable = DatabaseGradeTable(database)
      problemStorage = DatabaseProblemStorage(database)
      solutionDistributor = DatabaseSolutionDistributor(database)
      studentStorage = DatabaseStudentStorage(database)
      teacherStorage = DatabaseTeacherStorage(database)

      fillWithSamples(
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    }
    println("Startup time: ${startupTime.toFloat() / 1000.0} s")
    assertTrue(startupTime < 12000)

    val transactionsTime = measureTimeMillis {
      transaction {
        val course = coursesDistributor.getCourses().first()
        coursesDistributor.getStudents(course.id)
        coursesDistributor.getTeachers(course.id)
        problemStorage.getProblemsFromCourse(course.id)
        assignmentStorage.getAssignmentsForCourse(course.id)
      }
    }
    println("Transactions time: ${transactionsTime.toFloat() / 1000.0} s")
    assertTrue(transactionsTime < 300)
  }
}
