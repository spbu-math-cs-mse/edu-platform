package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

@Suppress("TooManyFunctions") // ok, as it is a database access class
internal interface CourseStorage {
  fun addStudentToCourse(studentId: StudentId, courseId: CourseId): Result<Unit, EduPlatformError>

  fun addTeacherToCourse(teacherId: TeacherId, courseId: CourseId): Result<Unit, EduPlatformError>

  fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError>

  fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError>

  fun getCourses(): Result<List<Course>, EduPlatformError>

  fun getStudentCourses(studentId: StudentId): Result<List<Course>, EduPlatformError>

  fun getTeacherCourses(teacherId: TeacherId): Result<List<Course>, EduPlatformError>

  fun resolveCourse(courseId: CourseId): Result<Course, EduPlatformError>

  fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, EduPlatformError>

  fun setCourseGroup(courseId: CourseId, rawChatId: RawChatId): Result<Unit, EduPlatformError>

  fun resolveCourseGroup(courseId: CourseId): Result<RawChatId?, EduPlatformError>

  fun updateCourseSpreadsheetId(
    courseId: CourseId,
    spreadsheetId: SpreadsheetId,
  ): Result<Unit, EduPlatformError>

  fun createCourse(description: String): Result<CourseId, EduPlatformError>

  fun getStudents(courseId: CourseId): Result<List<Student>, EduPlatformError>

  fun getTeachers(courseId: CourseId): Result<List<Teacher>, EduPlatformError>
}
