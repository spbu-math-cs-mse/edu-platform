package com.github.heheteam.samplebot.data

import Student

interface CoursesDistributor {
  fun addRecord(studentId: String, courseId: String)
  fun getCourses(studentId: String): String
  fun getAvailableCourses(studentId: String): MutableList<Course>
}

data class Course(val id: String, val description: String)

val mockCoursesTable = mutableMapOf(
  "0" to Course("0", "Начала мат. анализа"),
  "1" to Course("1", "Теория вероятности"),
  "2" to Course("2", "Линейная алгебра"),
  "3" to Course("3", "Теория функции комплексной переменной"),
)

val mockStudentsTable = mutableMapOf(
  "0" to Student("0"),
  "1" to Student("1"),
  "2" to Student("2"),
  "3" to Student("3"),
)

val mockAvailableCoursesTable = mutableMapOf(
  "0" to mutableListOf(mockCoursesTable["0"]!!, mockCoursesTable["3"]!!),
  "1" to mutableListOf(mockCoursesTable["1"]!!, mockCoursesTable["2"]!!),
)

class MockCoursesDistributor : CoursesDistributor {
  private val students = mockStudentsTable
  private val courses = mockCoursesTable
  private val available = mockAvailableCoursesTable

  private val data = mutableMapOf(
    "0" to mutableListOf("1", "2"),
    "1" to mutableListOf("0", "3"),
  )

  override fun addRecord(studentId: String, courseId: String) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
      data[studentId] = mutableListOf()
    }
    if (!available.containsKey(studentId)) {
      available[studentId] = mockCoursesTable.values.toMutableList()
    }
    available[studentId]!!.removeIf { it.id == courseId }
    data[studentId]!!.add(courseId)
  }

  override fun getCourses(studentId: String): String {
    if (!data.containsKey(studentId)) {
      return "Вы не записаны ни на один курс!"
    }
    return data[studentId]!!.map { courses[it]!!.description }.joinToString(separator = "\n") { "- $it" }
  }

  override fun getAvailableCourses(studentId: String): MutableList<Course> {
    if (!available.containsKey(studentId)) {
      available[studentId] = mockCoursesTable.values.toMutableList()
    }
    return available[studentId]!!.toMutableList()
  }
}
