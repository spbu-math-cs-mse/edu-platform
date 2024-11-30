package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course

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
  )

  fun removeTeacherFromCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  )

  fun getCourses(): List<CourseId>

  fun getStudentCourses(studentId: StudentId): List<CourseId>

  fun getTeacherCourses(teacherId: TeacherId): List<CourseId>

  fun resolveCourse(id: CourseId): Course?

  fun createCourse(description: String): CourseId

  fun getStudents(courseId: CourseId): List<StudentId>

  fun getTeachers(courseId: CourseId): List<TeacherId>
}
