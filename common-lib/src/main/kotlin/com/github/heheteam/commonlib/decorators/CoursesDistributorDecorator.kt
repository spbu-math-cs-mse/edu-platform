package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.BindError
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.DeleteError
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CoursesDistributor
import com.github.heheteam.commonlib.interfaces.RatingRecorder
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import dev.inmo.tgbotapi.types.RawChatId

internal class CoursesDistributorDecorator(
  private val coursesDistributor: CoursesDistributor,
  private val ratingRecorder: RatingRecorder,
) : CoursesDistributor {
  override fun addStudentToCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, BindError<StudentId, CourseId>> =
    coursesDistributor.addStudentToCourse(studentId, courseId).also {
      ratingRecorder.updateRating(courseId)
    }

  override fun addTeacherToCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, BindError<TeacherId, CourseId>> =
    coursesDistributor.addTeacherToCourse(teacherId, courseId)

  override fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<StudentId>> =
    coursesDistributor.removeStudentFromCourse(studentId, courseId).also {
      ratingRecorder.updateRating(courseId)
    }

  override fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, DeleteError<TeacherId>> =
    coursesDistributor.removeTeacherFromCourse(teacherId, courseId)

  override fun getCourses(): List<Course> = coursesDistributor.getCourses()

  override fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor.getStudentCourses(studentId)

  override fun getTeacherCourses(teacherId: TeacherId): List<Course> =
    coursesDistributor.getTeacherCourses(teacherId)

  override fun resolveCourse(courseId: CourseId): Result<Course, ResolveError<CourseId>> =
    coursesDistributor.resolveCourse(courseId)

  override fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, ResolveError<CourseId>> =
    coursesDistributor.resolveCourseWithSpreadsheetId(courseId)

  override fun setCourseGroup(
    courseId: CourseId,
    rawChatId: RawChatId,
  ): Result<Unit, ResolveError<CourseId>> = coursesDistributor.setCourseGroup(courseId, rawChatId)

  override fun resolveCourseGroup(courseId: CourseId): Result<RawChatId?, ResolveError<CourseId>> =
    coursesDistributor.resolveCourseGroup(courseId)

  override fun updateCourseSpreadsheetId(courseId: CourseId, spreadsheetId: SpreadsheetId) =
    coursesDistributor.updateCourseSpreadsheetId(courseId, spreadsheetId)

  override fun createCourse(description: String): CourseId =
    coursesDistributor.createCourse(description).also {
      ratingRecorder.createRatingSpreadsheet(it).getError()?.also { err -> println(err) }
    }

  override fun getStudents(courseId: CourseId): List<Student> =
    coursesDistributor.getStudents(courseId)

  override fun getTeachers(courseId: CourseId): List<Teacher> =
    coursesDistributor.getTeachers(courseId)
}
