package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.BindError
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.DeleteError
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

@Suppress("TooManyFunctions") // ok, as it is a database access class
internal interface CourseStorage {
  fun addStudentToCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, BindError<StudentId, CourseId>>

  fun addTeacherToCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, BindError<TeacherId, CourseId>>

  fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<StudentId>>

  fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<TeacherId>>

  fun getCourses(): Result<List<Course>, EduPlatformError>

  fun getStudentCourses(studentId: StudentId): List<Course>

  fun getTeacherCourses(teacherId: TeacherId): List<Course>

  fun resolveCourse(courseId: CourseId): Result<Course, ResolveError<CourseId>>

  fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, ResolveError<CourseId>>

  fun setCourseGroup(courseId: CourseId, rawChatId: RawChatId): Result<Unit, ResolveError<CourseId>>

  fun resolveCourseGroup(courseId: CourseId): Result<RawChatId?, ResolveError<CourseId>>

  fun updateCourseSpreadsheetId(courseId: CourseId, spreadsheetId: SpreadsheetId)

  fun createCourse(description: String): CourseId

  fun getStudents(courseId: CourseId): List<Student>

  fun getTeachers(courseId: CourseId): List<Teacher>
}
