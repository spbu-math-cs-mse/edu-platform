package com.github.heheteam.adminbot

import Course
import MockGradeTable
import Student
import Teacher

val mockGradeTable = MockGradeTable()

val mockCourses: MutableMap<String, Course> = mutableMapOf(
  "Геома 1" to Course(
    mutableListOf(Teacher("1")), mutableListOf(Student("1")),
    "какое-то описание", mockGradeTable,
  ),
)

var mockTgUsername: String = ""

val mockStudents: MutableMap<String, String> = mutableMapOf(
  "1" to "@username_of_a_student",
  "2" to "@another_username",
)

val mockTeachers: MutableMap<String, String> = mutableMapOf(
  "1" to "@username_of_a_teacher",
  "2" to "@some_username",
)

val mockAdmins: MutableMap<String, String> by lazy {
  mutableMapOf(
    "1" to mockTgUsername,
  )
}
