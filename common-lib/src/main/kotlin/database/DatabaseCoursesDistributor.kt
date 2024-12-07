import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.tables.CourseTable
import com.github.heheteam.commonlib.database.tables.CourseTeachers
import com.github.michaelbull.result.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCoursesDistributor(
  val database: Database,
) : CoursesDistributor {
  init {
    transaction(database) {
      SchemaUtils.create(CourseTable)
      SchemaUtils.create(CourseStudents)
      SchemaUtils.create(CourseTeachers)
    }
  }

  override fun addStudentToCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, BindError<StudentId, CourseId>> =
    transaction(database) {
      val exists =
        CourseStudents
          .selectAll()
          .where((CourseStudents.courseId eq courseId.id) and (CourseStudents.studentId eq studentId.id))
          .map { 0L }
          .isNotEmpty()
      if (!exists) {
        Ok(Unit)

        runCatching {
          CourseStudents.insert {
            it[CourseStudents.studentId] = studentId.id
            it[CourseStudents.courseId] = courseId.id
          }
          Unit
        }.mapError { BindError(studentId, courseId) }
      } else {
        Err(BindError(studentId, courseId))
      }
    }

  override fun addTeacherToCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, BindError<TeacherId, CourseId>> =
    transaction(database) {
      val exists =
        CourseTeachers
          .selectAll()
          .where((CourseTeachers.courseId eq courseId.id) and (CourseTeachers.teacherId eq teacherId.id))
          .map { 0L }
          .isNotEmpty()
      if (!exists) {
        try {
          CourseTeachers.insert {
            it[CourseTeachers.teacherId] = teacherId.id
            it[CourseTeachers.courseId] = courseId.id
          }
          Ok(Unit)
        } catch (_: Throwable) {
          Err(BindError(teacherId, courseId))
        }
      } else {
        Err(BindError(teacherId, courseId))
      }
    }

  override fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<StudentId>> =
    transaction(database) {
      val deletedRows =
        CourseStudents.deleteWhere { (CourseStudents.studentId eq studentId.id) and (CourseStudents.courseId eq courseId.id) }
      if (deletedRows == 1) {
        Ok(Unit)
      } else {
        Err(DeleteError(studentId, deletedRows))
      }
    }

  override fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<TeacherId>> =
    transaction(database) {
      val deletedRows =
        CourseTeachers.deleteWhere { (CourseTeachers.teacherId eq teacherId.id) and (CourseTeachers.courseId eq courseId.id) }
      if (deletedRows == 1) {
        Ok(Unit)
      } else {
        Err(DeleteError(teacherId, deletedRows))
      }
    }

  override fun getCourses(): List<Course> =
    transaction(database) {
      CourseTable.selectAll().map {
        Course(
          it[CourseTable.id].value.toCourseId(),
          it[CourseTable.name],
        )
      }
    }

  override fun getStudentCourses(studentId: StudentId): List<Course> =
    transaction {
      CourseStudents
        .join(CourseTable, JoinType.INNER, onColumn = CourseTable.id, otherColumn = CourseStudents.courseId)
        .selectAll()
        .where { CourseStudents.studentId eq studentId.id }
        .map {
          Course(
            it[CourseStudents.courseId].value.toCourseId(),
            it[CourseTable.name].toString(),
          )
        }
    }

  override fun getTeacherCourses(teacherId: TeacherId): List<Course> =
    transaction {
      CourseTeachers
        .join(CourseTable, JoinType.INNER, onColumn = CourseTable.id, otherColumn = CourseTeachers.courseId)
        .selectAll()
        .where { CourseTeachers.teacherId eq teacherId.id }
        .map {
          Course(
            it[CourseTeachers.courseId].value.toCourseId(),
            it[CourseTable.name].toString(),
          )
        }
    }

  override fun resolveCourse(courseId: CourseId): Result<Course, ResolveError<CourseId>> =
    transaction(database) {
      val row =
        CourseTable.selectAll().where(CourseTable.id eq courseId.id).singleOrNull()
          ?: return@transaction Err(ResolveError(courseId))
      Ok(Course(courseId, row[CourseTable.name]))
    }

  override fun createCourse(description: String): CourseId =
    transaction(database) {
      CourseTable.insert {
        it[CourseTable.name] = description
      } get CourseTable.id
    }.value.toCourseId()

  override fun getStudents(courseId: CourseId): List<StudentId> =
    transaction(database) {
      CourseStudents
        .selectAll()
        .where(CourseStudents.courseId eq courseId.id)
        .map { it[CourseStudents.studentId].value.toStudentId() }
    }

  override fun getTeachers(courseId: CourseId): List<TeacherId> =
    transaction(database) {
      CourseTeachers
        .selectAll()
        .where(CourseTeachers.courseId eq courseId.id)
        .map { it[CourseTeachers.teacherId].value.toTeacherId() }
    }
}
