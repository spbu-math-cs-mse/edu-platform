package com.github.heheteam.commonlib

interface CoursesDistributor {
  fun addRecord(
    studentId: String,
    courseId: String,
  )

  fun getCoursesBulletList(studentId: String): String

  fun getCourses(studentId: String): List<Course>

  fun getTeacherCourses(teacherId: String): List<Course>
}
