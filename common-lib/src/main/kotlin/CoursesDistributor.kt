package com.github.heheteam.commonlib

import Course

interface CoursesDistributor {
  fun addRecord(
    studentId: String,
    courseId: String,
  )

  fun getCourses(studentId: String): String

  fun getListOfCourses(studentId: String): List<Course>

  fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>>
}
