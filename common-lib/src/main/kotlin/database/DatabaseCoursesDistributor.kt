import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.tables.CourseTable
import com.github.heheteam.commonlib.database.tables.CourseTeachers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCoursesDistributor(
  val database: Database,
) : CoursesDistributor {
  init {
    transaction(database) {
      SchemaUtils.create(CourseTable)
      SchemaUtils.create(CourseStudents)
    }
  }

  override fun addRecord(
    studentId: StudentId,
    courseId: CourseId,
  ) {
    transaction(database) {
      CourseStudents.insert {
        it[CourseStudents.studentId] = studentId.id
        it[CourseStudents.courseId] = courseId.id
      }
    }
  }

  override fun getCourses(): List<CourseId> = transaction(database) {
    CourseTable.selectAll().map { it[CourseTable.id].value.toCourseId() }
  }

  override fun getTeacherCourses(teacherId: TeacherId): List<CourseId> =
    transaction(database) {
      CourseTeachers.selectAll().where(CourseTeachers.teacherId eq teacherId.id)
        .map { it[CourseTeachers.courseId].value.toCourseId() }
    }

  override fun resolveCourse(id: CourseId): Course = transaction(database) {
    val row =
      CourseTable.selectAll().where(CourseTable.id eq id.id).singleOrNull()
    Course(id, row!!.get(CourseTable.description))
  }

  override fun createCourse(description: String): CourseId {
    return transaction(database) {
      CourseTable.insert {
        it[CourseTable.description] = description
      } get CourseTable.id
    }.value.toCourseId()
  }

  override fun getStudents(courseId: CourseId): List<StudentId> =
    transaction(database) {
      CourseStudents.selectAll().where(CourseStudents.courseId eq courseId.id)
        .map { it[CourseStudents.studentId].value.toStudentId() }
    }

  override fun getStudentCourses(studentId: StudentId): List<CourseId> {
    val courseIds = transaction {
      CourseStudents
        .selectAll()
        .where { CourseStudents.studentId eq studentId.id }
        .map { it[CourseStudents.courseId].value.toCourseId() }
    }
    return courseIds
  }
}
