import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.util.fillWithSamples
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import kotlin.test.Test

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
    assertTimeout(Duration.ofMillis(8000)) {
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

    assertTimeout(Duration.ofMillis(300)) {
      val course = coursesDistributor.getCourses().first()
      coursesDistributor.getStudents(course.id)
      coursesDistributor.getTeachers(course.id)
      problemStorage.getProblemsFromCourse(course.id)
      assignmentStorage.getAssignmentsForCourse(course.id)
    }
  }
}
