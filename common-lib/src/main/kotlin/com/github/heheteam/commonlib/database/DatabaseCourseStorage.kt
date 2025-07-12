package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.domain.RichCourse
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.repository.CourseRepository
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("TooManyFunctions") // ok, as it is a database access class
class DatabaseCourseStorage(private val courseRepository: CourseRepository) : CourseStorage {

  override fun addStudentToCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> = binding {
    transaction { // Ensures the entire operation is atomic
      val richCourse = courseRepository.findById(courseId).bind()
      richCourse.addStudent(studentId).bind()
      courseRepository.save(richCourse).bind()
    }
    Unit
  }

  override fun addTeacherToCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> = binding {
    transaction {
      val richCourse = courseRepository.findById(courseId).bind()
      richCourse.addTeacher(teacherId).bind() // Assuming addTeacher exists on RichCourse
      courseRepository.save(richCourse).bind()
    }
    Unit
  }

  override fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> = binding {
    transaction {
      val richCourse = courseRepository.findById(courseId).bind()
      richCourse.removeStudent(studentId)
      courseRepository.save(richCourse).bind()
    }
    Unit
  }

  override fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> = binding {
    transaction {
      val richCourse = courseRepository.findById(courseId).bind()
      richCourse.removeTeacher(teacherId).bind() // Assuming removeTeacher exists on RichCourse
      courseRepository.save(richCourse).bind()
    }
    Unit
  }

  override fun getCourses(): Result<List<Course>, EduPlatformError> {
    return transaction {
      courseRepository.findAll().map { richCourses -> richCourses.map { it.toLegacy() } }
    }
  }

  override fun getStudentCourses(studentId: StudentId): Result<List<Course>, EduPlatformError> {
    return transaction {
      courseRepository.findByStudent(studentId).map { richCourses ->
        richCourses.map { it.toLegacy() }
      }
    }
  }

  override fun getTeacherCourses(teacherId: TeacherId): Result<List<Course>, EduPlatformError> =
    catchingTransaction(null) {
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

  override fun resolveCourse(courseId: CourseId): Result<Course, EduPlatformError> {
    return transaction { courseRepository.findById(courseId).map { it.toLegacy() } }
  }

  override fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, EduPlatformError> {
    return transaction {
      courseRepository.findById(courseId).map { richCourse ->
        richCourse.spreadsheetId?.let { spreadsheetId ->
          Pair(richCourse.toLegacy(), spreadsheetId)
        }
          ?: error(
            "SpreadsheetId not found for course ${courseId.long}"
          ) // This should be handled by ResolveError
      }
    }
  }

  override fun setCourseGroup(
    courseId: CourseId,
    rawChatId: RawChatId,
  ): Result<Unit, EduPlatformError> = binding {
    transaction {
      val richCourse = courseRepository.findById(courseId).bind()
      richCourse.groupChatId = rawChatId
      courseRepository.save(richCourse).bind()
    }
    Unit
  }

  override fun resolveCourseGroup(courseId: CourseId): Result<RawChatId?, EduPlatformError> {
    return transaction { courseRepository.findById(courseId).map { it.groupChatId } }
  }

  override fun updateCourseSpreadsheetId(
    courseId: CourseId,
    spreadsheetId: SpreadsheetId,
  ): Result<Unit, EduPlatformError> = binding {
    transaction {
      val richCourse = courseRepository.findById(courseId).bind()
      richCourse.spreadsheetId = spreadsheetId
      courseRepository.save(richCourse).bind()
    }
    Unit
  }

  override fun getStudents(courseId: CourseId): Result<List<Student>, EduPlatformError> =
    catchingTransaction(null) { // will be removed after full repository all-rounder
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
    catchingTransaction(null) { // will be removed after full repository all-rounder
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

  override fun createCourse(description: String): Result<CourseId, EduPlatformError> = binding {
    transaction {
      val newCourseId =
        CourseId(System.currentTimeMillis()) // Generate a new ID, or use a sequence from DB
      val newRichCourse =
        RichCourse(
          id = newCourseId,
          description = description,
          spreadsheetId = null,
          groupChatId = null,
          students = mutableListOf(),
          teachers = mutableListOf(),
        )
      courseRepository.save(newRichCourse).bind()
      newCourseId
    }
  }
}
