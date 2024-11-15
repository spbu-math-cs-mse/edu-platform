package com.github.heheteam.commonlib

class MockCoursesDistributor : CoursesDistributor {
  private val students =
    mutableMapOf(
      "0" to Student("0"),
      "1" to Student("1"),
      "2" to Student("2"),
      "3" to Student("3"),
    )

  private val courses =
    mutableMapOf(
      "0" to Course("0", mutableListOf(), mutableListOf(), "Начала мат. анализа", MockGradeTable()),
      "1" to Course("1", mutableListOf(), mutableListOf(), "Теория вероятности", MockGradeTable()),
      "2" to Course("2", mutableListOf(), mutableListOf(), "Линейная алгебра", MockGradeTable()),
      "3" to Course("3", mutableListOf(), mutableListOf(), "ТФКП", MockGradeTable()),
    )

  private val available =
    mutableMapOf(
      "0" to
        mutableMapOf(
          "0" to Pair(courses["0"]!!, true),
          "1" to Pair(courses["1"]!!, false),
          "2" to Pair(courses["2"]!!, false),
          "3" to Pair(courses["3"]!!, true),
        ),
    )

  private val data =
    mutableMapOf(
      "0" to mutableListOf("1", "2"),
    )

  override fun addRecord(
    studentId: String,
    courseId: String,
  ) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
    }
    if (!data.containsKey(studentId)) {
      data[studentId] = mutableListOf()
    }
    buildCoursesForStudent(studentId)
    available[studentId]!![courseId] = Pair(courses[courseId]!!, false)
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
      available[studentId] =
        mutableMapOf<String, Pair<Course, Boolean>>().apply {
          courses.forEach { (key, course) ->
            this[key] = Pair(course, true)
          }
        }
    }
  }
}
