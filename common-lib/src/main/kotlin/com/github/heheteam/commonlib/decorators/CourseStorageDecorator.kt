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
import com.github.heheteam.commonlib.interfaces.CourseTokenStorage
import com.github.heheteam.commonlib.interfaces.RatingRecorder
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import dev.inmo.tgbotapi.types.RawChatId

@Suppress("TooManyFunctions") // ok, as it is a database access class
internal class CourseStorageDecorator(
  private val courseStorage: CourseStorage,
  private val ratingRecorder: RatingRecorder,
  private val tokenStorage: CourseTokenStorage,
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

  override fun getStudentCourses(studentId: StudentId): List<Course> =
    courseStorage.getStudentCourses(studentId)

  override fun getTeacherCourses(teacherId: TeacherId): List<Course> =
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

  override fun updateCourseSpreadsheetId(courseId: CourseId, spreadsheetId: SpreadsheetId) =
    courseStorage.updateCourseSpreadsheetId(courseId, spreadsheetId)

  override fun createCourse(description: String): CourseId =
    courseStorage.createCourse(description).also {
      tokenStorage.createToken(it)
      ratingRecorder.createRatingSpreadsheet(it).getError()?.also { err -> println(err) }
    }

  override fun getStudents(courseId: CourseId): List<Student> = courseStorage.getStudents(courseId)

  override fun getTeachers(courseId: CourseId): List<Teacher> = courseStorage.getTeachers(courseId)
}
