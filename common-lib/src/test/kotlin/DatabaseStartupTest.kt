import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.util.fillWithSamples
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

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
      database = Database.connect(
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
