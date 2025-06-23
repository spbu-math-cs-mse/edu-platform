package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.BindError
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.DeleteError
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.RatingRecorder
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.logic.CourseTokenService
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.types.RawChatId

@Suppress("TooManyFunctions") // ok, as it is a database access class
internal class CourseStorageDecorator(
  private val courseStorage: CourseStorage,
  private val ratingRecorder: RatingRecorder,
  private val courseTokenService: CourseTokenService,
) : CourseStorage {
  override fun addStudentToCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, BindError<StudentId, CourseId>> =
    courseStorage.addStudentToCourse(studentId, courseId).also {
      ratingRecorder.updateRating(courseId)
    }

  override fun addTeacherToCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, BindError<TeacherId, CourseId>> =
    courseStorage.addTeacherToCourse(teacherId, courseId)

  override fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<StudentId>> =
    courseStorage.removeStudentFromCourse(studentId, courseId).also {
      ratingRecorder.updateRating(courseId)
    }

  override fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<TeacherId>> =
    courseStorage.removeTeacherFromCourse(teacherId, courseId)

  override fun getCourses(): Result<List<Course>, EduPlatformError> = courseStorage.getCourses()

  override fun getStudentCourses(studentId: StudentId): Result<List<Course>, EduPlatformError> =
    courseStorage.getStudentCourses(studentId)

  override fun getTeacherCourses(teacherId: TeacherId): Result<List<Course>, EduPlatformError> =
    courseStorage.getTeacherCourses(teacherId)

  override fun resolveCourse(courseId: CourseId): Result<Course, ResolveError<CourseId>> =
    courseStorage.resolveCourse(courseId)

  override fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, ResolveError<CourseId>> =
    courseStorage.resolveCourseWithSpreadsheetId(courseId)

  override fun setCourseGroup(
    courseId: CourseId,
    rawChatId: RawChatId,
  ): Result<Unit, ResolveError<CourseId>> = courseStorage.setCourseGroup(courseId, rawChatId)

  override fun resolveCourseGroup(courseId: CourseId): Result<RawChatId?, ResolveError<CourseId>> =
    courseStorage.resolveCourseGroup(courseId)

  override fun updateCourseSpreadsheetId(
    courseId: CourseId,
    spreadsheetId: SpreadsheetId,
  ): Result<Unit, EduPlatformError> =
    courseStorage.updateCourseSpreadsheetId(courseId, spreadsheetId)

  override fun createCourse(description: String): Result<CourseId, EduPlatformError> = binding {
    val courseId = courseStorage.createCourse(description).bind()
    courseTokenService.createToken(courseId)
    val spreadsheetId = ratingRecorder.createRatingSpreadsheet(courseId, description).bind()
    KSLog.info(
      "Created spreadsheet ${spreadsheetId.toUrl()} for course \"${description}\" (id: $courseId)"
    )
    courseId
  }

  override fun getStudents(courseId: CourseId): Result<List<Student>, EduPlatformError> =
    courseStorage.getStudents(courseId)

  override fun getTeachers(courseId: CourseId): Result<List<Teacher>, EduPlatformError> =
    courseStorage.getTeachers(courseId)
}
