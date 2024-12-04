package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course
import com.github.michaelbull.result.Result

interface CoursesDistributor {
  fun addStudentToCourse(
    studentId: StudentId,
    courseId: CourseId,
  )

  fun addTeacherToCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  )

  fun removeStudentFromCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DeleteError>

  fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, DeleteError>

  fun getCourses(): List<Course>

  fun getStudentCourses(studentId: StudentId): List<Course>

  fun getTeacherCourses(teacherId: TeacherId): List<Course>

  fun resolveCourse(id: CourseId): Result<Course, ResolveError>

  fun createCourse(description: String): CourseId

  fun getStudents(courseId: CourseId): List<StudentId>

  fun getTeachers(courseId: CourseId): List<TeacherId>
}
