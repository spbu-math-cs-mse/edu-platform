package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.BindError
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.DeleteError
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCoursesDistributor(val database: Database) : CoursesDistributor {
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
            (CourseStudents.courseId eq courseId.id) and (CourseStudents.studentId eq studentId.id)
          )
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
            (CourseTeachers.courseId eq courseId.id) and (CourseTeachers.teacherId eq teacherId.id)
          )
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
        CourseStudents.deleteWhere {
          (CourseStudents.studentId eq studentId.id) and (CourseStudents.courseId eq courseId.id)
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
          (CourseTeachers.teacherId eq teacherId.id) and (CourseTeachers.courseId eq courseId.id)
        }
      if (deletedRows == 1) {
        Ok(Unit)
      } else {
        Err(DeleteError(teacherId, deletedRows))
      }
    }

  override fun getCourses(): List<Course> =
    transaction(database) {
      CourseTable.selectAll().map {
        Course(it[CourseTable.id].value.toCourseId(), it[CourseTable.name])
      }
    }

  override fun getStudentCourses(studentId: StudentId): List<Course> = transaction {
    CourseStudents.join(
        CourseTable,
        JoinType.INNER,
        onColumn = CourseTable.id,
        otherColumn = CourseStudents.courseId,
      )
      .selectAll()
      .where { CourseStudents.studentId eq studentId.id }
      .map {
        Course(it[CourseStudents.courseId].value.toCourseId(), it[CourseTable.name].toString())
      }
  }

  override fun getTeacherCourses(teacherId: TeacherId): List<Course> = transaction {
    CourseTeachers.join(
        CourseTable,
        JoinType.INNER,
        onColumn = CourseTable.id,
        otherColumn = CourseTeachers.courseId,
      )
      .selectAll()
      .where { CourseTeachers.teacherId eq teacherId.id }
      .map {
        Course(it[CourseTeachers.courseId].value.toCourseId(), it[CourseTable.name].toString())
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
        CourseTable.insert { it[CourseTable.name] = description } get CourseTable.id
      }
      .value
      .toCourseId()

  override fun getStudents(courseId: CourseId): List<Student> =
    transaction(database) {
      CourseStudents.join(
          StudentTable,
          JoinType.INNER,
          onColumn = CourseStudents.studentId,
          otherColumn = StudentTable.id,
        )
        .selectAll()
        .where(CourseStudents.courseId eq courseId.id)
        .map {
          Student(
            it[CourseStudents.studentId].value.toStudentId(),
            it[StudentTable.name].toString(),
            it[StudentTable.surname].toString(),
          )
        }
    }

  override fun getTeachers(courseId: CourseId): List<Teacher> =
    transaction(database) {
      CourseTeachers.join(
          TeacherTable,
          JoinType.INNER,
          onColumn = CourseTeachers.teacherId,
          otherColumn = TeacherTable.id,
        )
        .selectAll()
        .where(CourseTeachers.courseId eq courseId.id)
        .map {
          Teacher(
            it[CourseTeachers.teacherId].value.toTeacherId(),
            it[TeacherTable.name].toString(),
            it[TeacherTable.surname].toString(),
            RawChatId(
              it[TeacherTable.tgId]
            )
          )
        }
    }
}
