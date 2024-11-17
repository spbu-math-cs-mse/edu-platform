package com.github.heheteam.commonlib

const val PROBLEMS_PER_COURSE = 4

class MockCoursesDistributor : CoursesDistributor {
  val singleUserId = "0"
  private val students =
    mutableMapOf(
      "0" to Student("0"),
      "1" to Student("1"),
      "2" to Student("2"),
      "3" to Student("3"),
    )

  /*
  generates a course with dummy content with PROBLEMS_PER_COURSE problems
   */
  fun generateCourse(courseId: String, name: String): Course {
    val singleAssignmentId = courseId + ".a1"
    return Course(
      courseId,
      mutableListOf(),
      mutableListOf(),
      name,
      MockGradeTable(),
      assignments = mutableListOf(
        Assignment(
          singleAssignmentId,
          "sample assignment",
          problems = (1..PROBLEMS_PER_COURSE).map {
            Problem(
              id = singleAssignmentId + "p$it",
              number = it.toString(),
              description = "",
              assignmentId = singleAssignmentId,
              maxScore = 1,
            )
          }.toMutableList(),
          courseId = "0",
        ),
      ),
    )
  }

  private val courses =
    mutableMapOf(
      "0" to generateCourse("0", "Начала мат. анализа"),
      "1" to generateCourse("1", "Теория вероятностей"),
      "2" to generateCourse("2", "Линейная алгебра"),
      "3" to generateCourse("3", "ТФКП"),
    )

  private val available =
    mutableMapOf(
      singleUserId to
        mutableMapOf(
          "0" to Pair(courses["0"]!!, true),
          "1" to Pair(courses["1"]!!, false),
          "2" to Pair(courses["2"]!!, false),
          "3" to Pair(courses["3"]!!, true),
        ),
    )

  private val data = mutableMapOf(
    singleUserId to mutableListOf("0", "2"),
    "1" to mutableListOf("1", "3"),
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

  override fun getCoursesBulletList(studentId: String): String {
    if (!data.containsKey(studentId)) {
      return "Вы не записаны ни на один курс!"
    }
    return data[studentId]!!.map { courses[it]!!.description }
      .joinToString(separator = "\n") { "- $it" }
  }

  override fun getCourses(studentId: String): List<Course> {
    if (!data.containsKey(studentId)) {
      return listOf()
    }
    return data[studentId]!!.map { courses[it]!! }
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
