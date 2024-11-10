package com.github.heheteam.studentbot.data

import com.github.heheteam.commonlib.Student

interface CoursesDistributor {
  fun addRecord(studentId: String, courseId: String)
  fun getCourses(studentId: String): String
  fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>>
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
  "0" to mutableMapOf(
    "0" to Pair(mockCoursesTable["0"]!!, true),
    "1" to Pair(mockCoursesTable["1"]!!, false),
    "2" to Pair(mockCoursesTable["2"]!!, false),
    "3" to Pair(mockCoursesTable["3"]!!, true),
  ),
  "1" to mutableMapOf(
    "0" to Pair(mockCoursesTable["0"]!!, false),
    "1" to Pair(mockCoursesTable["1"]!!, true),
    "2" to Pair(mockCoursesTable["2"]!!, true),
    "3" to Pair(mockCoursesTable["3"]!!, false),
  ),
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
    buildCoursesForStudent(studentId)
    available[studentId]!![courseId] = Pair(mockCoursesTable[courseId]!!, false)
    data[studentId]!!.add(courseId)
  }

  override fun getCourses(studentId: String): String {
    if (!data.containsKey(studentId)) {
      return "Вы не записаны ни на один курс!"
    }
    return data[studentId]!!.map { courses[it]!!.description }.joinToString(separator = "\n") { "- $it" }
  }

  override fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>> {
    buildCoursesForStudent(studentId)
    return available[studentId]!!.values.toMutableList()
  }

  private fun buildCoursesForStudent(studentId: String) {
    if (!available.containsKey(studentId)) {
      available[studentId] = mutableMapOf<String, Pair<Course, Boolean>>().apply {
        mockCoursesTable.forEach { (key, course) ->
          this[key] = Pair(course, true)
        }
      }
    }
  }
}
