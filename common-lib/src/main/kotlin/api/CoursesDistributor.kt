package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course

interface CoursesDistributor {
  fun addRecord(
    studentId: String,
    courseId: String,
  )

  fun getStudentCourses(studentId: String): List<Course>

  fun getCourses(): List<Course>

  fun getTeacherCourses(teacherId: String): List<Course>
}
