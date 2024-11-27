package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course

interface CoursesDistributor {
  fun addRecord(
    studentId: StudentId,
    courseId: CourseId,
  )

  fun getStudentCourses(studentId: StudentId): List<CourseId>

  fun getCourses(): List<CourseId>

  fun getTeacherCourses(teacherId: TeacherId): List<CourseId>

  fun resolveCourse(id: CourseId): Course?

  fun createCourse(description: Int): CourseId
}
