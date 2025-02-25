package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.BindError
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.DeleteError
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.SpreadsheetId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.michaelbull.result.Result

class CoursesDistributorDecorator(
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

  override fun updateCourseSpreadsheetId(courseId: CourseId, spreadsheetId: SpreadsheetId) =
    coursesDistributor.updateCourseSpreadsheetId(courseId, spreadsheetId)

  override fun createCourse(description: String): CourseId =
    coursesDistributor.createCourse(description).also { ratingRecorder.createRatingSpreadsheet(it) }

  override fun getStudents(courseId: CourseId): List<Student> =
    coursesDistributor.getStudents(courseId)

  override fun getTeachers(courseId: CourseId): List<Teacher> =
    coursesDistributor.getTeachers(courseId)
}
