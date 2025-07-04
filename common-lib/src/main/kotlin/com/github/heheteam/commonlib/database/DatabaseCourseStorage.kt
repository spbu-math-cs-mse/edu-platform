package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.errors.BindError
import com.github.heheteam.commonlib.errors.DeleteError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.errors.asEduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

@Suppress("TooManyFunctions") // ok, as it is a database access class
class DatabaseCourseStorage(val database: Database) : CourseStorage {
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
        CourseStudents.selectAll()
          .where(
            (CourseStudents.courseId eq courseId.long) and
              (CourseStudents.studentId eq studentId.long)
          )
          .map { 0L }
          .isNotEmpty()
      if (!exists) {
        Ok(Unit)
        runCatching {
            CourseStudents.insert {
              it[CourseStudents.studentId] = studentId.long
              it[CourseStudents.courseId] = courseId.long
            }
            Unit
          }
          .mapError { BindError(studentId, courseId) }
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
        CourseTeachers.selectAll()
          .where(
            (CourseTeachers.courseId eq courseId.long) and
              (CourseTeachers.teacherId eq teacherId.long)
          )
          .map { 0L }
          .isNotEmpty()
      if (!exists) {
        try {
          CourseTeachers.insert {
            it[CourseTeachers.teacherId] = teacherId.long
            it[CourseTeachers.courseId] = courseId.long
          }
          Ok(Unit)
        } catch (e: ExposedSQLException) {
          Err(
            BindError(
              teacherId,
              courseId,
              causedBy = e.asEduPlatformError(DatabaseCourseStorage::class),
            )
          )
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
        CourseStudents.deleteWhere {
          (CourseStudents.studentId eq studentId.long) and
            (CourseStudents.courseId eq courseId.long)
        }
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
        CourseTeachers.deleteWhere {
          (CourseTeachers.teacherId eq teacherId.long) and
            (CourseTeachers.courseId eq courseId.long)
        }
      if (deletedRows == 1) {
        Ok(Unit)
      } else {
        Err(DeleteError(teacherId, deletedRows))
      }
    }

  override fun getCourses(): Result<List<Course>, EduPlatformError> =
    transaction(database) {
        CourseTable.selectAll().map {
          Course(it[CourseTable.id].value.toCourseId(), it[CourseTable.name])
        }
      }
      .ok()

  override fun getStudentCourses(studentId: StudentId): Result<List<Course>, EduPlatformError> =
    catchingTransaction(database) {
      CourseStudents.join(
          CourseTable,
          JoinType.INNER,
          onColumn = CourseTable.id,
          otherColumn = CourseStudents.courseId,
        )
        .selectAll()
        .where { CourseStudents.studentId eq studentId.long }
        .map {
          Course(it[CourseStudents.courseId].value.toCourseId(), it[CourseTable.name].toString())
        }
    }

  override fun getTeacherCourses(teacherId: TeacherId): Result<List<Course>, EduPlatformError> =
    catchingTransaction(database) {
      CourseTeachers.join(
          CourseTable,
          JoinType.INNER,
          onColumn = CourseTable.id,
          otherColumn = CourseTeachers.courseId,
        )
        .selectAll()
        .where { CourseTeachers.teacherId eq teacherId.long }
        .map {
          Course(it[CourseTeachers.courseId].value.toCourseId(), it[CourseTable.name].toString())
        }
    }

  override fun resolveCourse(courseId: CourseId): Result<Course, ResolveError<CourseId>> =
    transaction(database) {
      val row =
        CourseTable.selectAll().where(CourseTable.id eq courseId.long).singleOrNull()
          ?: return@transaction Err(ResolveError(courseId))
      Ok(Course(courseId, row[CourseTable.name]))
    }

  override fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, ResolveError<CourseId>> =
    transaction(database) {
      val row =
        CourseTable.selectAll().where(CourseTable.id eq courseId.long).singleOrNull()
          ?: return@transaction Err(ResolveError(courseId))
      val spreadsheetId =
        row[CourseTable.spreadsheetId]
          ?: return@transaction Err(ResolveError<CourseId>(courseId, "SpreadsheetId"))
      Ok(Pair(Course(courseId, row[CourseTable.name]), SpreadsheetId(spreadsheetId)))
    }

  override fun setCourseGroup(
    courseId: CourseId,
    rawChatId: RawChatId,
  ): Result<Unit, ResolveError<CourseId>> =
    transaction(database) {
      val result =
        CourseTable.update({ CourseTable.id eq courseId.long }) {
          it[CourseTable.groupRawChatId] = rawChatId.long
        }
      if (result == 1) {
        Ok(Unit)
      } else {
        Err(ResolveError(courseId))
      }
    }

  override fun resolveCourseGroup(courseId: CourseId): Result<RawChatId?, ResolveError<CourseId>> =
    transaction(database) {
      val row =
        CourseTable.selectAll().where(CourseTable.id eq courseId.long).singleOrNull()
          ?: return@transaction Ok(null)
      val chatId =
        row[CourseTable.groupRawChatId]
          ?: return@transaction Err(ResolveError<CourseId>(courseId, "RawChatId"))
      Ok(RawChatId(chatId))
    }

  override fun updateCourseSpreadsheetId(
    courseId: CourseId,
    spreadsheetId: SpreadsheetId,
  ): Result<Unit, EduPlatformError> =
    catchingTransaction(database) {
      CourseTable.update({ CourseTable.id eq courseId.long }) {
        it[CourseTable.spreadsheetId] = spreadsheetId.long
      }
    }

  override fun createCourse(description: String): Result<CourseId, EduPlatformError> =
    catchingTransaction(database) {
        CourseTable.insert {
          it[CourseTable.name] = description
          it[CourseTable.spreadsheetId] = null
        } get CourseTable.id
      }
      .map { it.value.toCourseId() }

  override fun getStudents(courseId: CourseId): Result<List<Student>, EduPlatformError> =
    catchingTransaction(database) {
      CourseStudents.join(
          StudentTable,
          JoinType.INNER,
          onColumn = CourseStudents.studentId,
          otherColumn = StudentTable.id,
        )
        .selectAll()
        .where(CourseStudents.courseId eq courseId.long)
        .map {
          Student(
            it[CourseStudents.studentId].value.toStudentId(),
            it[StudentTable.name].toString(),
            it[StudentTable.surname].toString(),
            it[StudentTable.tgId].toRawChatId(),
          )
        }
    }

  override fun getTeachers(courseId: CourseId): Result<List<Teacher>, EduPlatformError> =
    catchingTransaction(database) {
      CourseTeachers.join(
          TeacherTable,
          JoinType.INNER,
          onColumn = CourseTeachers.teacherId,
          otherColumn = TeacherTable.id,
        )
        .selectAll()
        .where(CourseTeachers.courseId eq courseId.long)
        .map {
          Teacher(
            it[CourseTeachers.teacherId].value.toTeacherId(),
            it[TeacherTable.name].toString(),
            it[TeacherTable.surname].toString(),
            it[TeacherTable.tgId].toRawChatId(),
          )
        }
    }
}
