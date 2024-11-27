import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.database.tables.AssignmentTable
import com.github.heheteam.commonlib.database.tables.CourseStudents
import com.github.heheteam.commonlib.database.tables.CourseTable
import com.github.heheteam.commonlib.database.tables.ProblemTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCoursesDistributor(
  val database: Database,
  val gradeTable: GradeTable,
) : CoursesDistributor {
  override fun addRecord(studentId: Long, courseId: Long) {
    transaction(database) {
      CourseStudents.insert {
        it[CourseStudents.studentId] = studentId
        it[CourseStudents.courseId] = courseId
      }
    }
  }

  override fun getCourses(): List<Course> {
    TODO("Not yet implemented")
  }

  override fun getTeacherCourses(teacherId: Long): List<Course> {
    TODO("Not yet implemented")
  }

  override fun getStudentCourses(studentId: Long): List<Course> {
    val courses = transaction {
      val courseIds = CourseStudents.selectAll()
        .where { CourseStudents.studentId eq studentId }
        .map { it[CourseStudents.courseId] }
      val courses = courseIds.map { courseId ->
        val courseRow =
          CourseTable.selectAll().where { CourseTable.id eq courseId }.single()
        val courseDescription = courseRow[CourseTable.description]
        val preassignments = AssignmentTable
          .selectAll()
          .where { AssignmentTable.course eq courseId }
          .map { it[AssignmentTable.id] to it[AssignmentTable.description] }
          .toList()
        val assignments =
          preassignments.map { (assignmentId, assignmentDescription) ->
            queryAssignment(assignmentId, assignmentDescription, courseId)
          }
        Course(
          courseId.value,
          mutableListOf(),
          mutableListOf(),
          courseDescription,
          gradeTable,
          assignments.toMutableList(),
        )
      }
      courses
    }
    return courses
  }

  private fun queryAssignment(
    assignmentId: EntityID<Long>,
    assignmentDescription: String,
    courseId: EntityID<Long>,
  ): Assignment {
    val problems = ProblemTable.selectAll()
      .where { ProblemTable.assignment eq assignmentId }
      .map {
        Problem(
          it[ProblemTable.id].value,
          it[ProblemTable.id].value.toString(),
          it[ProblemTable.description],
          it[ProblemTable.maxScore],
          assignmentId.value,
        )
      }
    return Assignment(
      assignmentId.value,
      assignmentDescription,
      problems.toMutableList(),
      courseId.value,
    )
  }
}
