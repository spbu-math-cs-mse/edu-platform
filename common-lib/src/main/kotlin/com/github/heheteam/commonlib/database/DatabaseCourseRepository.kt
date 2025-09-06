package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.domain.RichCourse
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.repository.CourseRepository
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class DatabaseCourseRepository : CourseRepository {
  override fun save(course: RichCourse): Result<RichCourse, EduPlatformError> = binding {
    val courseIdLong = course.id.long
    val exists = CourseTable.selectAll().where { CourseTable.id eq courseIdLong }.count() > 0

    if (exists) {
      CourseTable.update({ CourseTable.id eq courseIdLong }) {
        it[CourseTable.name] = course.description
        it[CourseTable.spreadsheetId] = course.spreadsheetId?.string
        it[CourseTable.groupRawChatId] = course.groupChatId?.long
      }
    } else {
      CourseTable.insert {
        it[CourseTable.id] = courseIdLong
        it[CourseTable.name] = course.description
        it[CourseTable.spreadsheetId] = course.spreadsheetId?.string
        it[CourseTable.groupRawChatId] = course.groupChatId?.long
      }
    }

    // Synchronize students
    CourseStudents.deleteWhere { CourseStudents.courseId eq courseIdLong }
    if (course.students.isNotEmpty()) {
      CourseStudents.batchInsert(course.students) { studentId ->
        this[CourseStudents.courseId] = courseIdLong
        this[CourseStudents.studentId] = studentId.long
      }
    }

    // Synchronize teachers
    CourseTeachers.deleteWhere { CourseTeachers.courseId eq courseIdLong }
    if (course.teachers.isNotEmpty()) {
      CourseTeachers.batchInsert(course.teachers) { teacherId ->
        this[CourseTeachers.courseId] = courseIdLong
        this[CourseTeachers.teacherId] = teacherId.long
      }
    }

    course
  }

  override fun findById(courseId: CourseId): Result<RichCourse, EduPlatformError> {
    return CourseTable.selectAll()
      .where { CourseTable.id eq courseId.long }
      .singleOrNull()
      ?.let { row ->
        val students =
          CourseStudents.selectAll()
            .where { CourseStudents.courseId eq courseId.long }
            .map { StudentId(it[CourseStudents.studentId].value) }
            .toMutableList()

        val teachers =
          CourseTeachers.selectAll()
            .where { CourseTeachers.courseId eq courseId.long }
            .map { TeacherId(it[CourseTeachers.teacherId].value) }
            .toMutableList()

        Ok(
          RichCourse(
            id = CourseId(row[CourseTable.id].value),
            description = row[CourseTable.name],
            spreadsheetId = row[CourseTable.spreadsheetId]?.let { SpreadsheetId(it) },
            groupChatId = row[CourseTable.groupRawChatId]?.toRawChatId(),
            students = students,
            teachers = teachers,
          )
        )
      } ?: Err(NamedError("Bad!"))
  }

  override fun findAll(): Result<List<RichCourse>, EduPlatformError> {
    return Ok(
      CourseTable.selectAll().map { row ->
        val name = row[CourseTable.name]
        val courseId = CourseId(row[CourseTable.id].value)
        val students =
          CourseStudents.selectAll()
            .where { CourseStudents.courseId eq courseId.long }
            .map { StudentId(it[CourseStudents.studentId].value) }
            .toMutableList()

        val teachers =
          CourseTeachers.selectAll()
            .where { CourseTeachers.courseId eq courseId.long }
            .map { TeacherId(it[CourseTeachers.teacherId].value) }
            .toMutableList()

        RichCourse(
          id = courseId,
          description = name,
          spreadsheetId = row[CourseTable.spreadsheetId]?.let { SpreadsheetId(it) },
          groupChatId = row[CourseTable.groupRawChatId]?.toRawChatId(),
          students = students,
          teachers = teachers,
        )
      }
    )
  }

  override fun findByStudent(studentId: StudentId): Result<List<RichCourse>, EduPlatformError> {
    return Ok(
      CourseStudents.selectAll()
        .where { CourseStudents.studentId eq studentId.long }
        .map { CourseId(it[CourseStudents.courseId].value) }
        .mapNotNull { courseId -> findById(courseId).get() }
    )
  }
}
