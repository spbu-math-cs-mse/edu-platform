import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.CoursesDistributor
import com.github.heheteam.commonlib.database.tables.CourseStudents
import com.github.heheteam.commonlib.database.tables.CourseTable
import com.github.heheteam.commonlib.database.toIntIdHack
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCoursesDistributor(val database: Database) : CoursesDistributor {
  override fun addRecord(studentId: String, courseId: String) {
    transaction(database) {
      val id = CourseStudents.insert {
        it[CourseStudents.studentId] = studentId.toIntIdHack()
        it[CourseStudents.courseId] = courseId.toIntIdHack()
      }
    }
  }

  override fun getCoursesBulletList(studentId: String): String {
    TODO("Not yet implemented")
  }

  override fun getCourses(studentId: String): List<Course> {
    val courseRows = transaction {
      val courseIds = CourseStudents.selectAll()
        .where { CourseStudents.studentId eq studentId.toIntIdHack() }
        .map { it[CourseStudents.courseId] }
      val courseRows = courseIds.map { courseId ->
        CourseTable.selectAll().where { CourseTable.id eq courseId }.single()
      }
    }
    TODO("Not yet implemented")
  }

  override fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>> {
    TODO("Not yet implemented")
  }
}
