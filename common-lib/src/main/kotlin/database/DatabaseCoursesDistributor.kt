import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
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

  override fun getCourses(): List<CourseId> {
    TODO("Not yet implemented")
  }

  override fun getTeacherCourses(teacherId: TeacherId): List<CourseId> {
    TODO("Not yet implemented")
  }

  override fun resolveCourse(id: CourseId): Course {
    TODO("Not yet implemented")
  }

  override fun createCourse(description: String): CourseId {
    TODO("Not yet implemented")
  }

  override fun getStudents(courseId: CourseId): List<StudentId> {
    TODO("Not yet implemented")
  }

  override fun getStudentCourses(studentId: StudentId): List<CourseId> {
    val courses =
      transaction {
        val courseIds =
          CourseStudents
            .selectAll()
            .where { CourseStudents.studentId eq studentId.id }
            .map { it[CourseStudents.courseId] }
        val courses =
          courseIds.map { courseId ->
            val courseRow =
              CourseTable.selectAll().where { CourseTable.id eq courseId }.single()
            val courseDescription = courseRow[CourseTable.description]
            val preassignments =
              AssignmentTable
                .selectAll()
                .where { AssignmentTable.course eq courseId }
                .map { it[AssignmentTable.id] to it[AssignmentTable.description] }
                .toList()
            preassignments.map { (assignmentId, assignmentDescription) ->
              queryAssignment(assignmentId, assignmentDescription, courseId)
            }
            Course(
              CourseId(courseId.value),
              courseDescription,
            )
          }
        courses
      }
    return courses.map { it.id }
  }

  private fun queryAssignment(
    assignmentId: EntityID<Long>,
    assignmentDescription: String,
    courseId: EntityID<Long>,
  ): Assignment {
    val problems =
      ProblemTable
        .selectAll()
        .where { ProblemTable.assignment eq assignmentId }
        .map {
          Problem(
            ProblemId(it[ProblemTable.id].value),
            it[ProblemTable.id].value.toString(),
            it[ProblemTable.description],
            it[ProblemTable.maxScore],
            AssignmentId(assignmentId.value),
          )
        }
    return Assignment(
      AssignmentId(assignmentId.value),
      assignmentDescription,
      problems.map { it.id },
      CourseId(courseId.value),
    )
  }
}
