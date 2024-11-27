package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course

interface CoursesDistributor {
  fun addRecord(
    studentId: Long,
    courseId: Long,
  )

  fun getStudentCourses(studentId: Long): List<Course>

  fun getCourses(): List<Course>

  fun getTeacherCourses(teacherId: Long): List<Course>
}
