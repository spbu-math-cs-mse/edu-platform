import com.github.heheteam.commonlib.api.CoursesDistributor
import org.jetbrains.exposed.sql.Database
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
  val database =
    Database.connect(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver",
    )

  // assumes database is empty
  fun courseDistributorTests(coursesDistributor: CoursesDistributor) {
    val sampleDescription = "sample description"
    val id = coursesDistributor.createCourse(sampleDescription)
    val requiredId = coursesDistributor.getCourses().single().id
    assertEquals(id, requiredId)
    val resolvedCourse = coursesDistributor.resolveCourse(requiredId)!!
    assertEquals(sampleDescription, resolvedCourse.name)
  }

  @Test
  fun `course distributor works`() {
    val databaseGradeTable = DatabaseCoursesDistributor(database)
    courseDistributorTests(databaseGradeTable)
  }
}
