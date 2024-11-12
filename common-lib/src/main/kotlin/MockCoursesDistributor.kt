package com.github.heheteam.commonlib

class MockCoursesDistributor : CoursesDistributor {
  private val students = mockStudentsTable
  private val courses = mockCoursesTable
  private val available = mockAvailableCoursesTable

  private val data =
    mutableMapOf(
      "0" to mutableListOf("1", "2"),
      "1" to mutableListOf("0", "3"),
    )

  override fun addRecord(
    studentId: String,
    courseId: String,
  ) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
      data[studentId] = mutableListOf()
    }
    buildCoursesForStudent(studentId)
    available[studentId]!![courseId] = Pair(mockCoursesTable[courseId]!!, false)
    data[studentId]!!.add(courseId)
  }

  override fun getCourses(studentId: String): String {
    println(studentId)
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
      available[studentId] =
        mutableMapOf<String, Pair<Course, Boolean>>().apply {
          mockCoursesTable.forEach { (key, course) ->
            this[key] = Pair(course, true)
          }
        }
    }
  }
}
