package com.github.heheteam.commonlib

interface CoursesDistributor {
  fun addRecord(
    studentId: String,
    courseId: String,
  )

  fun getCourses(studentId: String): String

  fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>>
}
